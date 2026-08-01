package com.stayease.payment_service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stayease.payment_service.dto.Request.PaymentConfirmationRequest;
import com.stayease.payment_service.dto.Request.PaymentOrderRequest;
import com.stayease.payment_service.dto.Request.RefundRequest;
import com.stayease.payment_service.dto.Response.*;
import com.stayease.payment_service.entity.*;
import com.stayease.payment_service.exception.BusinessException;
import com.stayease.payment_service.exception.ResourceNotFoundException;
import com.stayease.payment_service.integrations.BookingServiceGateway;
import com.stayease.payment_service.repository.PaymentOrderRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentOrderRepository paymentOrderRepository;
    private final RazorpayVerificationService paymentVerificationService;
    private final RazorpayOrderService  razorpayOrderService;
    private final BookingServiceGateway bookingServiceGateway;
    private final AuditService auditService;
    private final RefundService refundService;
    private static final String DEFAULT_CURRENCY = "INR";
    private static final int MAX_RETRY_ATTEMPTS = 3;

    // ================= CREATE PAYMENT ORDER =================

    @Override
    @Transactional
    public PaymentOrderResponse createPaymentOrder(PaymentOrderRequest request){
        log.info("Creating payment order for booking: {}", request.getBookingId());
        if (request.getBookingId()==null){
            throw new BusinessException("Booking ID is required");
        }
        if (request.getAmount()==null || request.getAmount()<=0){
            throw new BusinessException("Invalid payment amount");
        }
        log.info("Creating payment order for booking: {}", request.getBookingId());
        Optional<PaymentOrder> existing=paymentOrderRepository.findByBookingId(request.getBookingId());
        if (existing.isPresent()){
            PaymentOrder payment=existing.get();
            switch(payment.getStatus()){
                case PAYMENT_CONFIRMED:
                case PENDING:
                case REFUND_PENDING:
                case REFUNDED:
                    return mapOrderToDTO(payment);
                case PAYMENT_FAILED:
                case EXPIRED:
                    paymentOrderRepository.delete(payment);
                    break;
                default:
                    break;
            }
        }
        try {
            RazorpayOrderResponse razorpayOrder=razorpayOrderService.createOrder(request);
            String razorpayOrderId=razorpayOrder.getId();
            PaymentOrder paymentOrder=PaymentOrder.builder()
                    .bookingId(request.getBookingId())
                    .amount(request.getAmount())
                    .razorpayOrderId(razorpayOrderId)
                    .status(PaymentOrderStatus.PENDING)
                    .userId(request.getUserId())
                    .currency(DEFAULT_CURRENCY)
                    .receiptNumber(generateReceiptNumber())
                    .paymentMethod(request.getPaymentMethod())
                    .attemptCount(0)
                    .createdAt(LocalDateTime.now())
                    .expiredAt(LocalDateTime.now().plusMinutes(15))
                    .build();
            PaymentOrder saved=paymentOrderRepository.save(paymentOrder);
            log.info("Payment order created successfully: {}", razorpayOrderId);
            return mapOrderToDTO(saved);
        }
        catch(BusinessException ex){
            throw ex;
        }
        catch(Exception ex){
            log.error("Payment creation failed", ex);
            throw new BusinessException("Unable to create payment order.");
        }
    }

    // ================= CONFIRM PAYMENT =================

    @Override
    @Transactional
    public PaymentResponse confirmPayment(PaymentConfirmationRequest request){
        log.info("Confirming payment for orderId: {}", request.getRazorpayOrderId());
        PaymentOrder paymentOrder=paymentOrderRepository.findByRazorpayOrderId(request.getRazorpayOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Payment order not found"));
        if (paymentOrder.getStatus()==PaymentOrderStatus.PAYMENT_CONFIRMED){
            return buildPaymentResponse(paymentOrder, "Already confirmed");
        }
        if(paymentOrder.getExpiredAt()!=null &&paymentOrder.getExpiredAt().isBefore(LocalDateTime.now())){
            failPaymentInternal(paymentOrder,"Payment order expired");
            throw new BusinessException("Payment session has expired.");
        }
        if (!paymentVerificationService.verifyPaymentSignature(request.getRazorpayOrderId(),request.getRazorpayPaymentId(),request.getRazorpaySignature())){
            failPaymentInternal(paymentOrder,"Invalid signature");
            throw new BusinessException("Payment verification failed");
        }
        try {
            RazorpayPaymentResponse payment=razorpayOrderService.getPaymentDetails(request.getRazorpayPaymentId());
            if (!"captured".equals(payment.getStatus())) {
                failPaymentInternal(paymentOrder, "Payment not captured");
                throw new BusinessException("Payment is not captured.");
            }
            long expectedAmount=Math.round(paymentOrder.getAmount() * 100);
            if (!payment.getAmount().equals(expectedAmount)){
                failPaymentInternal(paymentOrder,"Amount mismatch");
                throw new BusinessException("Payment amount mismatch.");
            }
            if (!paymentOrder.getCurrency().equalsIgnoreCase(payment.getCurrency())){
                failPaymentInternal(paymentOrder,"Currency mismatch");
                throw new BusinessException("Currency mismatch.");
            }
            if (!paymentOrder.getRazorpayOrderId().equals(payment.getOrder_id())){
                failPaymentInternal(paymentOrder,"Order mismatch");
                throw new BusinessException("Order mismatch.");
            }
            paymentOrder.setStatus(PaymentOrderStatus.PAYMENT_CONFIRMED);
            paymentOrder.setTransactionId(request.getRazorpayPaymentId());
            paymentOrder.setConfirmedAt(LocalDateTime.now());
            paymentOrderRepository.save(paymentOrder);
        } catch (Exception e) {
            failPaymentInternal(paymentOrder, e.getMessage());
            throw new BusinessException("Payment failed: " + e.getMessage());
        }
        try {
            bookingServiceGateway.confirmBooking(paymentOrder.getBookingId());
        } catch (Exception ex) {
            log.error("Booking sync failed after payment success", ex);
        }
        auditService.logEvent(paymentOrder.getRazorpayOrderId(),paymentOrder.getPaymentId(),"PAYMENT_CONFIRMED","Success");
        return buildPaymentResponse(paymentOrder,"Payment confirmed");
    }

    // ================= GET BY DB ID =================

    @Override
    public PaymentOrderResponse getPaymentOrderDetails(Long paymentId){
        log.info("Fetching payment order by DB id: {}", paymentId);
        PaymentOrder order=paymentOrderRepository.findById(paymentId).orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        return mapOrderToDTO(order);
    }

    // ================= GET BY RAZORPAY ID =================

    @Override
    public PaymentOrderResponse getByRazorpayOrderId(String razorpayOrderId){
        log.info("Fetching payment order by Razorpay id: {}",razorpayOrderId);
        PaymentOrder order=paymentOrderRepository.findByRazorpayOrderId(razorpayOrderId).orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        return mapOrderToDTO(order);
    }

    // ================= TEST CONFIRM PAYMENT =================

    @Override
    @Transactional
    public void testConfirmPayment(String razorpayOrderId) {
        log.warn("TEST PAYMENT TRIGGERED for orderId={}", razorpayOrderId);
        PaymentOrder paymentOrder=paymentOrderRepository.findByRazorpayOrderId(razorpayOrderId).orElseThrow(() -> new ResourceNotFoundException("Payment order not found"));
        if (paymentOrder.getStatus()!=PaymentOrderStatus.PENDING&& paymentOrder.getStatus()!=PaymentOrderStatus.PAYMENT_FAILED){
            throw new BusinessException("Payment cannot be confirmed.");
        }
        paymentOrder.setStatus(PaymentOrderStatus.PAYMENT_CONFIRMED);
        paymentOrder.setTransactionId("TEST_TXN_" + System.currentTimeMillis());
        paymentOrder.setConfirmedAt(LocalDateTime.now());
        paymentOrderRepository.save(paymentOrder);
        log.info("TEST PAYMENT SUCCESS for orderId={}, bookingId={}",razorpayOrderId, paymentOrder.getBookingId());
        try {
            bookingServiceGateway.confirmBooking(paymentOrder.getBookingId());
        } catch (Exception ex) {
            log.error("Booking sync failed (test)", ex);
        }
        auditService.logEvent(paymentOrder.getRazorpayOrderId(),paymentOrder.getPaymentId(), "TEST_PAYMENT_CONFIRMED", "Simulated success");
    }

    @Override
    public PaymentOrderResponse getPaymentByBookingId(Long bookingId){
        log.info("Fetching payment for bookingId={}",bookingId);
        PaymentOrder paymentOrder=paymentOrderRepository.findByBookingId(bookingId).orElseThrow(() ->new ResourceNotFoundException("Payment not found"));
        return mapOrderToDTO(paymentOrder);
    }

    @Override
    public String getPaymentStatus(Long bookingId){
        PaymentOrder paymentOrder = paymentOrderRepository.findByBookingId(bookingId).orElseThrow(() ->new ResourceNotFoundException("Payment not found"));
        return paymentOrder.getStatus().name();
    }

    @Override
    public ReceiptResponse getReceipt(Long paymentId){
        PaymentOrder paymentOrder=paymentOrderRepository.findById(paymentId).orElseThrow(() -> new ResourceNotFoundException("Payment not found"));
        if (paymentOrder.getStatus()!=PaymentOrderStatus.PAYMENT_CONFIRMED && paymentOrder.getStatus()!=PaymentOrderStatus.REFUNDED
                && paymentOrder.getStatus()!=PaymentOrderStatus.REFUND_PENDING){
            throw new BusinessException("Receipt is available only for completed payments.");
        }
        return ReceiptResponse.builder()
                .paymentId(paymentOrder.getPaymentId())
                .bookingId(paymentOrder.getBookingId())
                .userId(paymentOrder.getUserId())
                .receiptNumber(paymentOrder.getReceiptNumber())
                .razorpayOrderId(paymentOrder.getRazorpayOrderId())
                .transactionId(paymentOrder.getTransactionId())
                .amount(paymentOrder.getAmount())
                .refundAmount(paymentOrder.getRefundAmount())
                .currency(paymentOrder.getCurrency())
                .paymentMethod(paymentOrder.getPaymentMethod())
                .paymentStatus(paymentOrder.getStatus().name())
                .createdAt(paymentOrder.getCreatedAt())
                .confirmedAt(paymentOrder.getConfirmedAt())
                .refundedAt(paymentOrder.getRefundedAt())
                .build();
    }


    @Override
    @Transactional
    public PaymentResponse retryFailedPayment(Long paymentId){
        PaymentOrder paymentOrder=paymentOrderRepository.findById(paymentId).orElseThrow(() -> new ResourceNotFoundException("Payment not found"));
        if (paymentOrder.getStatus()!=PaymentOrderStatus.PAYMENT_FAILED){
            throw new BusinessException("Only failed payments can be retried.");
        }
        if (paymentOrder.getAttemptCount()>=MAX_RETRY_ATTEMPTS){
            throw new BusinessException("Maximum retry attempts exceeded.");
        }
        PaymentOrderRequest retryRequest=PaymentOrderRequest.builder()
                        .bookingId(paymentOrder.getBookingId())
                        .userId(paymentOrder.getUserId())
                        .amount(paymentOrder.getAmount())
                        .paymentMethod(paymentOrder.getPaymentMethod())
                        .build();
        RazorpayOrderResponse order=razorpayOrderService.createOrder(retryRequest);

        paymentOrder.setRazorpayOrderId(order.getId());
        paymentOrder.setStatus(PaymentOrderStatus.PENDING);
        paymentOrder.setExpiredAt(LocalDateTime.now().plusMinutes(15));
        paymentOrder.setAttemptCount(paymentOrder.getAttemptCount()+1);
        paymentOrder.setErrorMessage(null);
        paymentOrderRepository.save(paymentOrder);
        // Simulate payment retry
        return buildPaymentResponse(paymentOrder, "Retry initiated successfully.");
    }

    @Override
    @Transactional
    public List<PaymentHistoryResponse> getPaymentHistory(){
        Long userId = getCurrentUserId();
        return paymentOrderRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::mapToHistoryResponse)
                .toList();
    }

    @Override
    @Transactional
    public RefundResponse refundBooking(Long bookingId){
        log.info("Processing refund for bookingId={}",bookingId);
        PaymentOrder paymentOrder=paymentOrderRepository.findByBookingId(bookingId)
                .orElseThrow(() ->new ResourceNotFoundException("Payment not found"));
        RefundRequest request=RefundRequest.builder()
                .paymentId(paymentOrder.getPaymentId())
                .razorpayOrderId(paymentOrder.getRazorpayOrderId())
                .refundAmount(paymentOrder.getAmount())
                .currency(paymentOrder.getCurrency())
                .reason("Booking Cancelled")
                .build();
        RefundResponse refund=refundService.initiateRefund(request);
        refund=refundService.processRefund(refund.getRefundId());
        refundService.completeRefund(refund.getRefundId());
        return refundService.getRefundDetails(refund.getRefundId());
    }

    @Transactional
    @Override
    public void expirePendingPayments(){
        List<PaymentOrder> expiredPayments=paymentOrderRepository.findByStatusAndExpiredAtBefore(PaymentOrderStatus.PENDING,LocalDateTime.now());
        for (PaymentOrder payment:expiredPayments){
            payment.setStatus(PaymentOrderStatus.EXPIRED);
            paymentOrderRepository.save(payment);
            try {
                bookingServiceGateway.failBooking(payment.getBookingId());
            }
            catch(Exception ex){
                log.error("Failed to update booking after payment expiry. bookingId={}",payment.getBookingId(),ex);
            }
            auditService.logEvent(payment.getRazorpayOrderId(),payment.getPaymentId(),"PAYMENT_EXPIRED","Payment expired automatically after timeout.");
            log.info("Payment expired successfully. paymentId={}",payment.getPaymentId());
        }
    }

    // ================= WEBHOOK =================

    @Override
    @Transactional
    public void handleWebhookCallback(String rawPayload, String signature){
        try {
            if (!paymentVerificationService.verifyWebhookSignature(rawPayload, signature)){
                throw new BusinessException("Invalid webhook signature");
            }
            ObjectMapper mapper=new ObjectMapper();
            WebhookPayload payload=mapper.readValue(rawPayload, WebhookPayload.class);
            String event=payload.getEvent();
            Map<String, Object> paymentObj=(Map<String, Object>) payload.getPayload().get("payment");
            Map<String, Object> entity=(Map<String, Object>) paymentObj.get("entity");
            String razorpayOrderId=(String) entity.get("order_id");
            PaymentOrder paymentOrder=paymentOrderRepository.findByRazorpayOrderId(razorpayOrderId)
                    .orElseThrow(() -> new ResourceNotFoundException("Payment order not found"));
            if(paymentOrder.getStatus()==PaymentOrderStatus.PAYMENT_CONFIRMED){
                log.warn("Webhook already processed for orderId={}", razorpayOrderId);
                return;
            }
            if("payment.captured".equals(event)){
                paymentOrder.setStatus(PaymentOrderStatus.PAYMENT_CONFIRMED);
                paymentOrder.setConfirmedAt(LocalDateTime.now());
                paymentOrderRepository.save(paymentOrder);
                try {
                    bookingServiceGateway.confirmBooking(paymentOrder.getBookingId());
                }
                catch (Exception ex){
                    log.error("Booking sync failed (webhook)", ex);
                }
                auditService.logEvent(razorpayOrderId,paymentOrder.getPaymentId(),"WEBHOOK_PAYMENT_CONFIRMED","Captured via webhook");
            }
            else if("payment.failed".equals(event)){
                if(paymentOrder.getStatus() != PaymentOrderStatus.PAYMENT_CONFIRMED){
                    failPaymentInternal(paymentOrder, "Webhook failure event");
                }
            }
            else{
                log.info("Unhandled webhook event: {}", event);
            }
        }
        catch(Exception e){
            throw new BusinessException("Webhook processing failed: "+ e.getMessage());
        }
    }

    private String generateReceiptNumber(){
        return "STAY-PAY-" + UUID.randomUUID().toString().substring(0,8).toUpperCase();
    }

    private Long getCurrentUserId(){
        Authentication authentication=SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()){
            throw new BusinessException("No authenticated user.");
        }
        return Long.parseLong(authentication.getName());
    }

    // ================= FAILURE =================

    private void failPaymentInternal(PaymentOrder paymentOrder,String error){
        paymentOrder.setStatus(PaymentOrderStatus.PAYMENT_FAILED);
        paymentOrder.setErrorMessage(error);
        paymentOrderRepository.save(paymentOrder);
        try {
            bookingServiceGateway.failBooking(paymentOrder.getBookingId());
        }
        catch (Exception ex){
            log.error("Booking fail sync failed", ex);
        }
    }

    // ================= MAPPERS =================
    private PaymentOrderResponse mapOrderToDTO(PaymentOrder order){
        return PaymentOrderResponse.builder()
                .paymentId(order.getPaymentId())
                .bookingId(order.getBookingId())
                .userId(order.getUserId())
                .amount(order.getAmount())
                .refundAmount(order.getRefundAmount())
                .currency(order.getCurrency())
                .receiptNumber(order.getReceiptNumber())
                .razorpayOrderId(order.getRazorpayOrderId())
                .status(order.getStatus().name())
                .paymentMethod(order.getPaymentMethod())
                .createdAt(order.getCreatedAt())
                .confirmedAt(order.getConfirmedAt())
                .refundedAt(order.getRefundedAt())
                .build();
    }

    private PaymentResponse buildPaymentResponse(PaymentOrder order,String message){
        return PaymentResponse.builder()
                .razorpayOrderId(order.getRazorpayOrderId())
                .bookingId(order.getBookingId())
                .paymentId(order.getPaymentId())
                .amount(order.getAmount())
                .paymentStatus(order.getStatus().name())
                .transactionId(order.getTransactionId())
                .build();
    }

    private PaymentHistoryResponse mapToHistoryResponse(PaymentOrder payment){
        return PaymentHistoryResponse.builder()
                .paymentId(payment.getPaymentId())
                .bookingId(payment.getBookingId())
                .amount(payment.getAmount())
                .refundAmount(payment.getRefundAmount())
                .paymentStatus(payment.getStatus().name())
                .paymentMethod(payment.getPaymentMethod())
                .currency(payment.getCurrency())
                .receiptNumber(payment.getReceiptNumber())
                .transactionId(payment.getTransactionId())
                .createdAt(payment.getCreatedAt())
                .confirmedAt(payment.getConfirmedAt())
                .refundedAt(payment.getRefundedAt())
                .build();
    }
}