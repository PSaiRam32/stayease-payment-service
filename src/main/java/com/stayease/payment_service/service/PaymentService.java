package com.stayease.payment_service.service;

import com.stayease.payment_service.dto.*;

public interface PaymentService {

    // ================= ORDER =================

    PaymentOrderResponseDTO createPaymentOrder(PaymentOrderRequestDTO request);

    PaymentOrderResponseDTO getPaymentOrderDetails(Long paymentId);

    PaymentOrderResponseDTO getByRazorpayOrderId(String razorpayOrderId);

    // ================= PAYMENT =================

    PaymentResponseDTO confirmPayment(PaymentConfirmationRequestDTO request);

    void handleWebhookCallback(String rawPayload, String signature);

    // ================= TEST MODE =================

    void testConfirmPayment(String razorpayOrderId);
}