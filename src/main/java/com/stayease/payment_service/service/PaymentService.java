package com.stayease.payment_service.service;

import com.stayease.payment_service.dto.*;

import java.util.List;

public interface PaymentService {

    // ================= ORDER =================

    PaymentOrderResponse createPaymentOrder(PaymentOrderRequest request);

    PaymentOrderResponse getPaymentOrderDetails(Long paymentId);

    PaymentOrderResponse getByRazorpayOrderId(String razorpayOrderId);
    List<PaymentHistoryResponse> getPaymentHistory();
    PaymentResponseDTO retryFailedPayment(Long paymentId);
    PaymentOrderResponse getPaymentByBookingId(Long bookingId);
    RefundResponseDTO refundBooking(Long bookingId);
    void expirePendingPayments();
    ReceiptResponseDTO getReceipt(Long paymentId);
    String getPaymentStatus(Long bookingId);

    // ================= PAYMENT =================

    PaymentResponseDTO confirmPayment(PaymentConfirmationRequestDTO request);

    void handleWebhookCallback(String rawPayload, String signature);

    // ================= TEST MODE =================

    void testConfirmPayment(String razorpayOrderId);
}