package com.stayease.payment_service.service;

import com.stayease.payment_service.config.BookingServiceClient;
import com.stayease.payment_service.config.NotificationClient;
import com.stayease.payment_service.config.UserClient;
import com.stayease.payment_service.dto.*;
import com.stayease.payment_service.entity.*;
import com.stayease.payment_service.exception.BusinessException;
import com.stayease.payment_service.exception.ResourceNotFoundException;
import com.stayease.payment_service.repository.PaymentOrderRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentOrderRepository paymentOrderRepository;
    private final RazorpayIntegrationService razorpayService;
    private final BookingServiceClient bookingServiceClient;
    private final NotificationClient notificationClient;
    private final UserClient userClient;
    private final AuditService auditService;

    // ================= CREATE PAYMENT ORDER =================

    @Override
    @Transactional
    public PaymentOrderResponseDTO createPaymentOrder(PaymentOrderRequestDTO request) {
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
                    .paymentMethod(request.getPaymentMethod())
                    .attemptCount(0)
                    .createdAt(LocalDateTime.now())
                    .expiredAt(LocalDateTime.now().plusMinutes(15))
                    .build();
            return mapOrderToDTO(paymentOrderRepository.save(paymentOrder));
        } catch (Exception e) {
            throw new BusinessException("Payment order creation failed: " + e.getMessage());
        }
    }

    // ================= CONFIRM PAYMENT =================

    @Override
    @Transactional
    public PaymentResponseDTO confirmPayment(PaymentConfirmationRequestDTO request) {
        PaymentOrder paymentOrder = paymentOrderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Payment order not found"));
        if (paymentOrder.getStatus() == PaymentOrderStatus.PAYMENT_CONFIRMED) {
            return buildPaymentResponse(paymentOrder, "Already confirmed");
        }
        UserResponseDTO user = userClient.getUserByBookingId(paymentOrder.getBookingId());
        if (!razorpayService.verifyPaymentSignature(
                request.getRazorpayOrderId(),
                request.getRazorpayPaymentId(),
                request.getRazorpaySignature())) {

            failPayment(paymentOrder, "Invalid signature", user);
            throw new BusinessException("Payment verification failed");
        }
        try {
            razorpayService.getPaymentDetails(request.getRazorpayPaymentId());
            paymentOrder.setStatus(PaymentOrderStatus.PAYMENT_CONFIRMED);
            paymentOrder.setTransactionId(request.getRazorpayPaymentId());
            paymentOrder.setConfirmedAt(LocalDateTime.now());
            paymentOrderRepository.save(paymentOrder);
            bookingServiceClient.confirmBooking(paymentOrder.getBookingId());
            sendNotification(user,
                    paymentOrder.getBookingId(),
                    "BOOKING_CONFIRMED",
                    "Your booking is confirmed. Booking ID: " + paymentOrder.getBookingId());
            auditService.logEvent(paymentOrder.getId(), "PAYMENT_CONFIRMED", "Success");
            return buildPaymentResponse(paymentOrder, "Payment confirmed");
        } catch (Exception e) {
            failPayment(paymentOrder, e.getMessage(), user);
            throw new BusinessException("Payment failed: " + e.getMessage());
        }
    }

    // ================= TEST CONFIRM PAYMENT =================

    @Override
    @Transactional
    public void testConfirmPayment(String razorpayOrderId) {
        PaymentOrder paymentOrder = paymentOrderRepository.findByRazorpayOrderId(razorpayOrderId)
                 .orElseThrow(() -> new ResourceNotFoundException("Payment order not found"));
        if (paymentOrder.getStatus() == PaymentOrderStatus.PAYMENT_CONFIRMED) {
            log.warn("Already confirmed orderId={}", razorpayOrderId);
            return;
        }
        paymentOrder.setStatus(PaymentOrderStatus.PAYMENT_CONFIRMED);
        paymentOrder.setTransactionId("TEST_TXN_" + System.currentTimeMillis());
        paymentOrder.setConfirmedAt(LocalDateTime.now());
        paymentOrderRepository.save(paymentOrder);
        log.info("TEST PAYMENT SUCCESS for orderId={}, bookingId={}",
                razorpayOrderId, paymentOrder.getBookingId());
        bookingServiceClient.confirmBooking(paymentOrder.getBookingId());
        try {
            UserResponseDTO user = userClient.getUserByBookingId(paymentOrder.getBookingId());
            sendNotification(user,
                    paymentOrder.getBookingId(),
                    "BOOKING_CONFIRMED",
                    "Your booking is confirmed. Booking ID: " + paymentOrder.getBookingId());
        } catch (Exception ex) {
            log.error("Notification failed", ex);
        }
        auditService.logEvent(paymentOrder.getId(), "TEST_PAYMENT_CONFIRMED", "Simulated success");
    }

    // ================= WEBHOOK =================

    @Override
    @Transactional
    public void handleWebhookCallback(WebhookPayloadDTO payload) {
        try {
            String event = payload.getEvent();
            Map<String, Object> payment = (Map<String, Object>) payload.getPayload().get("payment");
            String razorpayOrderId = (String) payment.get("order_id");
            PaymentOrder paymentOrder = paymentOrderRepository
                    .findByRazorpayOrderId(razorpayOrderId)
                    .orElseThrow(() -> new ResourceNotFoundException("Payment order not found"));
            UserResponseDTO user = userClient.getUserByBookingId(paymentOrder.getBookingId());
            if ("payment.authorized".equals(event)) {
                paymentOrder.setStatus(PaymentOrderStatus.PAYMENT_CONFIRMED);
                paymentOrder.setConfirmedAt(LocalDateTime.now());
                bookingServiceClient.confirmBooking(paymentOrder.getBookingId());
                sendNotification(user,
                        paymentOrder.getBookingId(),
                        "BOOKING_CONFIRMED",
                        "Payment confirmed via webhook");
            } else {
                failPayment(paymentOrder, "Webhook failed", user);
            }
            paymentOrderRepository.save(paymentOrder);
        } catch (Exception e) {
            throw new BusinessException("Webhook processing failed: " + e.getMessage());
        }
    }

    // ================= GET BY DB ID =================

    @Override
    public PaymentOrderResponseDTO getPaymentOrderDetails(Long id) {
        PaymentOrder order = paymentOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        return mapOrderToDTO(order);
    }

    // ================= GET BY RAZORPAY ID =================

    @Override
    public PaymentOrderResponseDTO getByRazorpayOrderId(String razorpayOrderId) {
        PaymentOrder order = paymentOrderRepository
                .findByRazorpayOrderId(razorpayOrderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        return mapOrderToDTO(order);
    }

    // ================= FAILURE =================

    private void failPayment(PaymentOrder paymentOrder, String error, UserResponseDTO user) {
        paymentOrder.setStatus(PaymentOrderStatus.PAYMENT_FAILED);
        paymentOrder.setErrorMessage(error);
        paymentOrderRepository.save(paymentOrder);
        bookingServiceClient.failBooking(paymentOrder.getBookingId());
        sendNotification(user,
                paymentOrder.getBookingId(),
                "PAYMENT_FAILED",
                "Payment failed for booking ID: " + paymentOrder.getBookingId());
    }

    // ================= NOTIFICATION =================

    private void sendNotification(UserResponseDTO user, Long bookingId, String type, String message) {
        NotificationRequestDTO notification = new NotificationRequestDTO();
        notification.setBookingId(bookingId);
        notification.setEmail(user.getEmail());
        notification.setPhoneNumber(user.getPhone());
        notification.setType(type);
        notification.setMessage(message);
        notification.setChannels(List.of("EMAIL", "SMS"));
        notificationClient.sendNotification(notification);
    }

    // ================= MAPPERS =================

    private PaymentOrderResponseDTO mapOrderToDTO(PaymentOrder order) {
        return PaymentOrderResponseDTO.builder()
                .id(order.getId())
                .bookingId(order.getBookingId())
                .amount(order.getAmount())
                .razorpayOrderId(order.getRazorpayOrderId())
                .status(order.getStatus().name())
                .build();
    }

    private PaymentResponseDTO buildPaymentResponse(PaymentOrder order, String message) {
        return PaymentResponseDTO.builder()
                .id(order.getId())
                .paymentId(order.getId())
                .bookingId(order.getBookingId())
                .amount(order.getAmount())
                .paymentStatus(order.getStatus().name())
                .transactionId(order.getTransactionId())
                .build();
    }
}