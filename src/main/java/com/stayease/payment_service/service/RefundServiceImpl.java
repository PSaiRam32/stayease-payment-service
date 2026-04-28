package com.stayease.payment_service.service;

import com.stayease.payment_service.dto.RefundRequestDTO;
import com.stayease.payment_service.dto.RefundResponseDTO;
import com.stayease.payment_service.entity.PaymentOrder;
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
import java.util.UUID;

/**
 * Service for handling payment refunds
 * Manages refund requests, processing, and tracking
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RefundServiceImpl implements RefundService {

    private final PaymentOrderRepository paymentOrderRepository;
    private final RefundTransactionRepository refundTransactionRepository;
    private final RazorpayIntegrationService razorpayService;
    private final AuditService auditService;

    @Override
    @Transactional
    public RefundResponseDTO initiateRefund(RefundRequestDTO request) {
        log.info("Initiating refund for order: {}", request.getRazorpayOrderId());
        PaymentOrder paymentOrder = paymentOrderRepository.findByRazorpayOrderId(request.getRazorpayOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Payment order not found"));
        // Validate refund eligibility
        if (request.getRefundAmount() > paymentOrder.getAmount()) {
            log.warn("Refund amount exceeds payment amount for order: {}", request.getRazorpayOrderId());
            throw new BusinessException("Refund amount cannot exceed payment amount");
        }
        // Check if refund already exists
        if (refundTransactionRepository.existsByrazorpayOrderId(request.getRazorpayOrderId())) {
            log.warn("Refund already completed for order: {}", request.getRazorpayOrderId());
            throw new BusinessException("Refund already processed for this payment");
        }
        try {
            // Create refund record
            RefundTransaction refund = RefundTransaction.builder()
                    .razorpayOrderId(paymentOrder.getRazorpayOrderId())
                    .refundId(Long.valueOf("refund_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12)))
                    .amount(request.getRefundAmount())
                    .currency(request.getCurrency() != null ? request.getCurrency() : "INR")
                    .reason(request.getReason())
                    .status("INITIATED")
                    .createdAt(LocalDateTime.now())
                    .build();
            refund = refundTransactionRepository.save(refund);
            log.info("Refund initiated with ID: {}", refund.getRefundId());
            // Audit log
            auditService.logEvent(paymentOrder.getRazorpayOrderId(), paymentOrder.getPaymentId(),"REFUND_INITIATED",
                    "Refund initiated for amount: " + request.getRefundAmount());
            return RefundResponseDTO.builder()
                    .refundId(refund.getRefundId())
                    .razorpayOrderId(paymentOrder.getRazorpayOrderId())
                    .refundId(refund.getRefundId())
                    .amount(request.getRefundAmount())
                    .status("INITIATED")
                    .reason(request.getReason())
                    .createdAt(refund.getCreatedAt())
                    .build();

        } catch (Exception e) {
            log.error("Failed to initiate refund for order: {}", request.getRazorpayOrderId(), e);
            throw new BusinessException("Failed to initiate refund: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public RefundResponseDTO processRefund(Long refundId) {
        log.info("Processing refund: {}", refundId);
        RefundTransaction refund = refundTransactionRepository.findById(refundId)
                .orElseThrow(() -> new ResourceNotFoundException("Refund not found"));
        if (!"INITIATED".equals(refund.getStatus())) {
            log.warn("Cannot process refund with status: {}", refund.getStatus());
            throw new BusinessException("Refund cannot be processed in current status");
        }

        try {
            PaymentOrder paymentOrder = paymentOrderRepository.findByRazorpayOrderId(refund.getRazorpayOrderId())
                    .orElseThrow(() -> new ResourceNotFoundException("Payment order not found"));
            // Call Razorpay refund API (if transactionId exists)
            if (paymentOrder.getTransactionId() != null) {
                // In production, call actual Razorpay refund endpoint
                log.debug("Processing refund via Razorpay for transaction: {}", paymentOrder.getTransactionId());
            }
            refund.setStatus("PROCESSING");
            refund.setProcessedAt(LocalDateTime.now());
            refund = refundTransactionRepository.save(refund);
            auditService.logEvent(paymentOrder.getRazorpayOrderId(),paymentOrder.getPaymentId(), "REFUND_PROCESSING",
                    "Refund processing started for amount: " + refund.getAmount());
            log.info("Refund processing initiated: {}", refundId);
            return RefundResponseDTO.builder()
                    .refundId(refund.getRefundId())
                    .razorpayOrderId(refund.getRazorpayOrderId())
                    .refundId(refund.getRefundId())
                    .amount(refund.getAmount())
                    .status("PROCESSING")
                    .reason(refund.getReason())
                    .createdAt(refund.getCreatedAt())
                    .processedAt(refund.getProcessedAt())
                    .build();
        } catch (Exception e) {
            refund.setStatus("FAILED");
            refund.setFailureReason(e.getMessage());
            refundTransactionRepository.save(refund);
            log.error("Failed to process refund: {}", refundId, e);
            throw new BusinessException("Failed to process refund: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public void completeRefund(Long refundId) {
        log.info("Completing refund: {}", refundId);
        RefundTransaction refund = refundTransactionRepository.findById(refundId)
                .orElseThrow(() -> new ResourceNotFoundException("Refund not found"));
        refund.setStatus("COMPLETED");
        refund.setCompletedAt(LocalDateTime.now());
        refundTransactionRepository.save(refund);
        auditService.logEvent(refund.getRazorpayOrderId(),refund.getPaymentId(), "REFUND_COMPLETED",
                "Refund completed for amount: " + refund.getAmount());
        log.info("Refund completed: {}", refundId);
    }

    @Override
    public RefundResponseDTO getRefundDetails(Long refundId) {
        log.info("Fetching refund details: {}", refundId);
        RefundTransaction refund = refundTransactionRepository.findById(refundId)
                .orElseThrow(() -> new ResourceNotFoundException("Refund not found"));
        return RefundResponseDTO.builder()
                .refundId(refund.getRefundId())
                .razorpayOrderId(refund.getRazorpayOrderId())
                .refundId(refund.getRefundId())
                .amount(refund.getAmount())
                .status(refund.getStatus())
                .reason(refund.getReason())
                .createdAt(refund.getCreatedAt())
                .processedAt(refund.getProcessedAt())
                .completedAt(refund.getCompletedAt())
                .failureReason(refund.getFailureReason())
                .build();
    }
}

