package com.stayease.payment_service.service;

import com.stayease.payment_service.dto.Request.RefundRequest;
import com.stayease.payment_service.dto.Response.RefundResponse;
import com.stayease.payment_service.entity.PaymentOrder;
import com.stayease.payment_service.entity.PaymentOrderStatus;
import com.stayease.payment_service.entity.RefundStatus;
import com.stayease.payment_service.entity.RefundTransaction;
import com.stayease.payment_service.exception.BusinessException;
import com.stayease.payment_service.exception.ResourceNotFoundException;
import com.stayease.payment_service.repository.PaymentOrderRepository;
import com.stayease.payment_service.repository.RefundTransactionRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Service for handling payment refunds
 * Manages refund requests, processing, and tracking
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RefundServiceImpl implements RefundService{

    private final PaymentOrderRepository paymentOrderRepository;
    private final RefundTransactionRepository refundTransactionRepository;
    private final RazorpayIntegrationService razorpayService;
    private final AuditService auditService;

    @Override
    @Transactional
    public RefundResponse initiateRefund(RefundRequest request){
        log.info("Initiating refund for order: {}",request.getRazorpayOrderId());
        PaymentOrder paymentOrder=paymentOrderRepository.findByRazorpayOrderId(request.getRazorpayOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Payment order not found"));
        // Validate refund eligibility
        // 1. Validate payment status
        if (paymentOrder.getStatus()!=PaymentOrderStatus.PAYMENT_CONFIRMED){
            throw new BusinessException("Only confirmed payments can be refunded.");
        }
        // 2. Validate refund amount
        if (request.getRefundAmount()>paymentOrder.getAmount()){
            throw new BusinessException("Refund amount cannot exceed payment amount");
        }
        // 3. Check duplicate refund
        if (refundTransactionRepository.existsByrazorpayOrderId(request.getRazorpayOrderId())){
            throw new BusinessException("Refund already processed for this payment");
        }
        paymentOrder.setStatus(PaymentOrderStatus.REFUND_PENDING);
        paymentOrderRepository.save(paymentOrder);
        try {
            // Create refund record
            RefundTransaction refund=RefundTransaction.builder()
                    .razorpayOrderId(paymentOrder.getRazorpayOrderId())
                    .amount(request.getRefundAmount())
                    .currency(request.getCurrency() != null ? request.getCurrency() : "INR")
                    .reason(request.getReason())
                    .status(RefundStatus.INITIATED)
                    .createdAt(LocalDateTime.now())
                    .build();
            refund=refundTransactionRepository.save(refund);
            log.info("Refund initiated with ID: {}", refund.getRefundId());
            // Audit log
            auditService.logEvent(paymentOrder.getRazorpayOrderId(), paymentOrder.getPaymentId(),"REFUND_INITIATED",
                    "Refund initiated for amount: " + request.getRefundAmount());
            return RefundResponse.builder()
                    .refundId(refund.getRefundId())
                    .razorpayOrderId(paymentOrder.getRazorpayOrderId())
                    .amount(request.getRefundAmount())
                    .status("INITIATED")
                    .reason(request.getReason())
                    .createdAt(refund.getCreatedAt())
                    .build();
        } catch (Exception e){
            log.error("Failed to initiate refund for order: {}", request.getRazorpayOrderId(), e);
            throw new BusinessException("Failed to initiate refund: " + e.getMessage());
        }
    }

    @Override
    public RefundResponse getRefundDetails(Long refundId){
        log.info("Fetching refund details: {}", refundId);
        RefundTransaction refund=refundTransactionRepository.findById(refundId)
                .orElseThrow(() -> new ResourceNotFoundException("Refund not found"));
        return RefundResponse.builder()
                .refundId(refund.getRefundId())
                .razorpayOrderId(refund.getRazorpayOrderId())
                .amount(refund.getAmount())
                .status(refund.getStatus().name())
                .reason(refund.getReason())
                .createdAt(refund.getCreatedAt())
                .processedAt(refund.getProcessedAt())
                .completedAt(refund.getCompletedAt())
                .failureReason(refund.getFailureReason())
                .build();
    }


    @Override
    @Transactional
    public RefundResponse processRefund(Long refundId){
        log.info("Processing refund: {}",refundId);
        RefundTransaction refund=refundTransactionRepository.findById(refundId)
                .orElseThrow(() -> new ResourceNotFoundException("Refund not found"));
        if (refund.getStatus()!=RefundStatus.INITIATED){
            throw new BusinessException("Refund cannot be processed in current status.");
        }
        try {
            PaymentOrder paymentOrder=paymentOrderRepository.findByRazorpayOrderId(refund.getRazorpayOrderId())
                    .orElseThrow(() -> new ResourceNotFoundException("Payment order not found"));
            // Call Razorpay refund API (if transactionId exists)
            if (paymentOrder.getTransactionId()!=null) {
                // In production, call actual Razorpay refund endpoint
                log.debug("Processing refund via Razorpay for transaction: {}", paymentOrder.getTransactionId());
            }
            refund.setStatus(RefundStatus.PROCESSING);
            refund.setProcessedAt(LocalDateTime.now());
            refund = refundTransactionRepository.save(refund);
            auditService.logEvent(paymentOrder.getRazorpayOrderId(),paymentOrder.getPaymentId(), "REFUND_PROCESSING",
                    "Refund processing started for amount: " + refund.getAmount());
            log.info("Refund processing initiated: {}", refundId);
            return RefundResponse.builder()
                    .refundId(refund.getRefundId())
                    .razorpayOrderId(refund.getRazorpayOrderId())
                    .amount(refund.getAmount())
                    .status("PROCESSING")
                    .reason(refund.getReason())
                    .createdAt(refund.getCreatedAt())
                    .processedAt(refund.getProcessedAt())
                    .build();
        } catch (Exception e){
            refund.setStatus(RefundStatus.FAILED);
            refund.setFailureReason(e.getMessage());
            refundTransactionRepository.save(refund);
            log.error("Failed to process refund: {}", refundId, e);
            throw new BusinessException("Failed to process refund: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public void completeRefund(Long refundId){
        log.info("Completing refund: {}",refundId);
        RefundTransaction refund=refundTransactionRepository.findById(refundId)
                .orElseThrow(() -> new ResourceNotFoundException("Refund not found"));
        PaymentOrder paymentOrder=paymentOrderRepository.findByRazorpayOrderId(refund.getRazorpayOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Payment order not found"));
        paymentOrder.setStatus(PaymentOrderStatus.REFUNDED);
        paymentOrder.setRefundAmount(refund.getAmount());
        paymentOrder.setRefundedAt(LocalDateTime.now());
        paymentOrder.setUpdatedAt(LocalDateTime.now());
        paymentOrderRepository.save(paymentOrder);
        refund.setStatus(RefundStatus.COMPLETED);
        refund.setCompletedAt(LocalDateTime.now());
        refundTransactionRepository.save(refund);
        auditService.logEvent(refund.getRazorpayOrderId(),refund.getPaymentId(), "REFUND_COMPLETED",
                "Refund completed for amount: " + refund.getAmount());
        log.info("Refund completed: {}", refundId);
    }
}

