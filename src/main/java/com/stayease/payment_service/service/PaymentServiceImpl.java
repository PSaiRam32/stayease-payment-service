package com.stayease.payment_service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stayease.payment_service.config.BookingServiceClient;
import com.stayease.payment_service.dto.*;
import com.stayease.payment_service.entity.*;
import com.stayease.payment_service.exception.BusinessException;
import com.stayease.payment_service.exception.ResourceNotFoundException;
import com.stayease.payment_service.repository.PaymentOrderRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentOrderRepository paymentOrderRepository;
    private final RazorpayIntegrationService razorpayService;
    private final BookingServiceClient bookingServiceClient;
    private final AuditService auditService;
    private final RefundService refundService;
    private static final String DEFAULT_CURRENCY = "INR";
    private static final int MAX_RETRY_ATTEMPTS = 3;

    @Retry(name = "bookingRetry")
    @CircuitBreaker(name = "bookingCB", fallbackMethod = "bookingConfirmFallback")
    private void confirmBookingSafe(Long bookingId) {
        bookingServiceClient.confirmBooking(bookingId);
    }

    private void bookingConfirmFallback(Long bookingId, Throwable ex) {
        log.error("Booking confirm FAILED after payment success. bookingId={}", bookingId, ex);
        // TODO: push to retry queue / DB
    }

    @Retry(name = "bookingRetry")
    @CircuitBreaker(name = "bookingCB", fallbackMethod = "bookingFailFallback")
    private void failBookingSafe(Long bookingId) {
        bookingServiceClient.failBooking(bookingId);
    }

    private void bookingFailFallback(Long bookingId, Throwable ex) {
        log.error("Booking fail FAILED. bookingId={}", bookingId, ex);
        // TODO: push to retry queue / DB (future improvement)
    }

    // ================= CREATE PAYMENT ORDER =================

    @Override
    @Transactional
    public PaymentOrderResponse createPaymentOrder(PaymentOrderRequest request) {
        if (request.getBookingId() == null) {
            throw new BusinessException("Booking ID is required");
        }
        if (request.getAmount() == null || request.getAmount() <= 0) {
            throw new BusinessException("Invalid payment amount");
        }
        log.info("Creating payment order for booking: {}", request.getBookingId());
        paymentOrderRepository.findByBookingId(request.getBookingId())
                .ifPresent(order -> {
                    if (order.getStatus() != PaymentOrderStatus.EXPIRED &&
                            order.getStatus() != PaymentOrderStatus.CANCELLED &&
                            order.getStatus() != PaymentOrderStatus.PAYMENT_FAILED) {

                        throw new BusinessException("Payment order already exists");
                    }
                });

        try {
            Map<String, Object> razorpayOrder = razorpayService.createOrder(request);
            String razorpayOrderId = (String) razorpayOrder.get("id");
            PaymentOrder paymentOrder = PaymentOrder.builder()
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
            PaymentOrder saved = paymentOrderRepository.save(paymentOrder);
            log.info("Payment order created successfully: {}", razorpayOrderId);
            return mapOrderToDTO(saved);
        } catch (Exception e) {
            throw new BusinessException("Payment order creation failed: " + e.getMessage());
        }
    }
    private String generateReceiptNumber() {
        return "STAY-PAY-" + System.currentTimeMillis();
    }

    private Long getCurrentUserId(){
        Authentication authentication=SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BusinessException("No authenticated user.");
        }
        return Long.parseLong(authentication.getName());
    }
    @Override
    @Transactional
    public List<PaymentHistoryResponse> getPaymentHistory(){
        Long userId = getCurrentUserId();
        return paymentOrderRepository
                .findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::mapToHistoryResponse)
                .toList();
    }

    @Override
    @Transactional
    public PaymentResponseDTO retryFailedPayment(Long paymentId){
        PaymentOrder paymentOrder = paymentOrderRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));
        if (paymentOrder.getStatus() != PaymentOrderStatus.PAYMENT_FAILED){
            throw new BusinessException("Only failed payments can be retried.");
        }
        if (paymentOrder.getAttemptCount() >= MAX_RETRY_ATTEMPTS){
            throw new BusinessException("Maximum retry attempts exceeded.");
        }
        paymentOrder.setAttemptCount(paymentOrder.getAttemptCount()+1);
        paymentOrder.setErrorMessage(null);
        paymentOrder.setExpiredAt(LocalDateTime.now().plusMinutes(15));
        paymentOrder.setStatus(PaymentOrderStatus.PENDING);
        paymentOrderRepository.save(paymentOrder);
        // Simulate payment retry
        return buildPaymentResponse(paymentOrder, "Retry initiated successfully.");
    }

    // ================= CONFIRM PAYMENT =================

    @Override
    @Transactional
    public PaymentResponseDTO confirmPayment(PaymentConfirmationRequestDTO request) {
        PaymentOrder paymentOrder = paymentOrderRepository.findByRazorpayOrderId(request.getRazorpayOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Payment order not found"));
        if (paymentOrder.getStatus() == PaymentOrderStatus.PAYMENT_CONFIRMED) {
            return buildPaymentResponse(paymentOrder, "Already confirmed");
        }
        if (!razorpayService.verifyPaymentSignature(
                request.getRazorpayOrderId(),
                request.getRazorpayPaymentId(),
                request.getRazorpaySignature())) {

            failPaymentInternal(paymentOrder, "Invalid signature");
            throw new BusinessException("Payment verification failed");
        }
        try {
            razorpayService.getPaymentDetails(request.getRazorpayPaymentId());
            paymentOrder.setStatus(PaymentOrderStatus.PAYMENT_CONFIRMED);
            paymentOrder.setTransactionId(request.getRazorpayPaymentId());
            paymentOrder.setConfirmedAt(LocalDateTime.now());
            paymentOrderRepository.save(paymentOrder);
        } catch (Exception e) {
            failPaymentInternal(paymentOrder, e.getMessage());
            throw new BusinessException("Payment failed: " + e.getMessage());
        }
        try {
            confirmBookingSafe(paymentOrder.getBookingId());
        } catch (Exception ex) {
            log.error("Booking sync failed after payment success", ex);
        }

        auditService.logEvent(
                paymentOrder.getRazorpayOrderId(),
                paymentOrder.getPaymentId(),
                "PAYMENT_CONFIRMED",
                "Success"
        );

        return buildPaymentResponse(paymentOrder, "Payment confirmed");
    }

    // ================= TEST CONFIRM PAYMENT =================

    @Override
    @Transactional
    public void testConfirmPayment(String razorpayOrderId) {
        PaymentOrder paymentOrder = paymentOrderRepository.findByRazorpayOrderId(razorpayOrderId)
                 .orElseThrow(() -> new ResourceNotFoundException("Payment order not found"));
        if (paymentOrder.getStatus() != PaymentOrderStatus.PENDING
                    && paymentOrder.getStatus() != PaymentOrderStatus.PAYMENT_FAILED) {
            throw new BusinessException("Payment cannot be confirmed.");
        }
        paymentOrder.setStatus(PaymentOrderStatus.PAYMENT_CONFIRMED);
        paymentOrder.setTransactionId("TEST_TXN_" + System.currentTimeMillis());
        paymentOrder.setConfirmedAt(LocalDateTime.now());
        paymentOrderRepository.save(paymentOrder);
        log.info("TEST PAYMENT SUCCESS for orderId={}, bookingId={}",
                razorpayOrderId, paymentOrder.getBookingId());
        try {
            confirmBookingSafe(paymentOrder.getBookingId());
        } catch (Exception ex) {
            log.error("Booking sync failed (test)", ex);
        }
        auditService.logEvent(paymentOrder.getRazorpayOrderId(),paymentOrder.getPaymentId(), "TEST_PAYMENT_CONFIRMED", "Simulated success");
    }

    // ================= WEBHOOK =================

    @Override
    @Transactional
    public void handleWebhookCallback(String rawPayload, String signature) {
        try {
            if (!razorpayService.verifyWebhookSignature(rawPayload, signature)) {
                throw new BusinessException("Invalid webhook signature");
            }
            ObjectMapper mapper = new ObjectMapper();
            WebhookPayloadDTO payload = mapper.readValue(rawPayload, WebhookPayloadDTO.class);
            String event = payload.getEvent();
            Map<String, Object> paymentObj =
                    (Map<String, Object>) payload.getPayload().get("payment");
            Map<String, Object> entity =
                    (Map<String, Object>) paymentObj.get("entity");
            String razorpayOrderId = (String) entity.get("order_id");
            PaymentOrder paymentOrder = paymentOrderRepository
                    .findByRazorpayOrderId(razorpayOrderId)
                    .orElseThrow(() -> new ResourceNotFoundException("Payment order not found"));
            if (paymentOrder.getStatus() == PaymentOrderStatus.PAYMENT_CONFIRMED) {
                log.warn("Webhook already processed for orderId={}", razorpayOrderId);
                return;
            }
            if ("payment.captured".equals(event)) {
                paymentOrder.setStatus(PaymentOrderStatus.PAYMENT_CONFIRMED);
                paymentOrder.setConfirmedAt(LocalDateTime.now());
                paymentOrderRepository.save(paymentOrder);
                try {
                    confirmBookingSafe(paymentOrder.getBookingId());
                } catch (Exception ex) {
                    log.error("Booking sync failed (webhook)", ex);
                }
                auditService.logEvent(
                        razorpayOrderId,
                        paymentOrder.getPaymentId(),
                        "WEBHOOK_PAYMENT_CONFIRMED",
                        "Captured via webhook"
                );

            } else if ("payment.failed".equals(event)) {
                if (paymentOrder.getStatus() != PaymentOrderStatus.PAYMENT_CONFIRMED) {
                    failPaymentInternal(paymentOrder, "Webhook failure event");
                }
            } else {
                log.info("Unhandled webhook event: {}", event);
            }
        } catch (Exception e) {
            throw new BusinessException("Webhook processing failed: " + e.getMessage());
        }
    }

    // ================= GET BY DB ID =================

    @Override
    public PaymentOrderResponse getPaymentOrderDetails(Long id) {
        PaymentOrder order = paymentOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        return mapOrderToDTO(order);
    }

    // ================= GET BY RAZORPAY ID =================

    @Override
    public PaymentOrderResponse getByRazorpayOrderId(String razorpayOrderId) {
        PaymentOrder order = paymentOrderRepository
                .findByRazorpayOrderId(razorpayOrderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        return mapOrderToDTO(order);
    }

    // ================= FAILURE =================

    private void failPaymentInternal(PaymentOrder paymentOrder, String error) {
        paymentOrder.setStatus(PaymentOrderStatus.PAYMENT_FAILED);
        paymentOrder.setErrorMessage(error);
        paymentOrderRepository.save(paymentOrder);
        try {
            failBookingSafe(paymentOrder.getBookingId());
        } catch (Exception ex) {
            log.error("Booking fail sync failed", ex);
        }
    }

    @Override
    public PaymentOrderResponse getPaymentByBookingId(Long bookingId) {

        PaymentOrder paymentOrder = paymentOrderRepository
                .findByBookingId(bookingId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Payment not found"));

        return mapOrderToDTO(paymentOrder);
    }

    @Override
    @Transactional
    public RefundResponseDTO refundBooking(Long bookingId) {

        log.info("Processing refund for bookingId={}", bookingId);

        PaymentOrder paymentOrder = paymentOrderRepository
                .findByBookingId(bookingId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Payment not found"));

        RefundRequestDTO request = RefundRequestDTO.builder()
                .paymentId(paymentOrder.getPaymentId())
                .razorpayOrderId(paymentOrder.getRazorpayOrderId())
                .refundAmount(paymentOrder.getAmount())
                .currency(paymentOrder.getCurrency())
                .reason("Booking Cancelled")
                .build();

        RefundResponseDTO refund = refundService.initiateRefund(request);

        refund = refundService.processRefund(refund.getRefundId());

        refundService.completeRefund(refund.getRefundId());

        return refundService.getRefundDetails(refund.getRefundId());
    }

    // ================= MAPPERS =================

    private PaymentOrderResponse mapOrderToDTO(PaymentOrder order) {
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

    private PaymentResponseDTO buildPaymentResponse(PaymentOrder order, String message) {
        return PaymentResponseDTO.builder()
                .razorpayOrderId(order.getRazorpayOrderId())
                .bookingId(order.getBookingId())
                .paymentId(order.getPaymentId())
                .amount(order.getAmount())
                .paymentStatus(order.getStatus().name())
                .transactionId(order.getTransactionId())
                .build();
    }

    private PaymentHistoryResponse mapToHistoryResponse(PaymentOrder payment) {
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
    @Transactional
    @Override
    public void expirePendingPayments() {

        List<PaymentOrder> expiredPayments =
                paymentOrderRepository
                        .findByStatusAndExpiredAtBefore(
                                PaymentOrderStatus.PENDING,
                                LocalDateTime.now());

        for (PaymentOrder payment : expiredPayments) {

            payment.setStatus(PaymentOrderStatus.EXPIRED);

            paymentOrderRepository.save(payment);

            try {

                failBookingSafe(payment.getBookingId());

            } catch (Exception ex) {

                log.error(
                        "Failed to update booking after payment expiry. bookingId={}",
                        payment.getBookingId(),
                        ex);
            }

            auditService.logEvent(
                    payment.getRazorpayOrderId(),
                    payment.getPaymentId(),
                    "PAYMENT_EXPIRED",
                    "Payment expired automatically after timeout.");

            log.info(
                    "Payment expired successfully. paymentId={}",
                    payment.getPaymentId());
        }
    }

    @Override
    public ReceiptResponseDTO getReceipt(Long paymentId) {
        PaymentOrder paymentOrder = paymentOrderRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));

        if (paymentOrder.getStatus() != PaymentOrderStatus.PAYMENT_CONFIRMED
                && paymentOrder.getStatus() != PaymentOrderStatus.REFUNDED
                && paymentOrder.getStatus() != PaymentOrderStatus.REFUND_PENDING) {

            throw new BusinessException(
                    "Receipt is available only for completed payments.");
        }

        return ReceiptResponseDTO.builder()
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
    public String getPaymentStatus(Long bookingId) {

        PaymentOrder paymentOrder = paymentOrderRepository
                .findByBookingId(bookingId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Payment not found"));

        return paymentOrder.getStatus().name();
    }
}