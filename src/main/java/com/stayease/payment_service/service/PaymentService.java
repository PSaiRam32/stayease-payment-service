package com.stayease.payment_service.service;

import com.stayease.payment_service.dto.Request.PaymentConfirmationRequest;
import com.stayease.payment_service.dto.Request.PaymentOrderRequest;
import com.stayease.payment_service.dto.Response.*;

import java.util.List;

public interface PaymentService {

    PaymentOrderResponse createPaymentOrder(PaymentOrderRequest request);
    PaymentResponse confirmPayment(PaymentConfirmationRequest request);
    PaymentOrderResponse getPaymentOrderDetails(Long paymentId);
    PaymentOrderResponse getByRazorpayOrderId(String razorpayOrderId);
    void testConfirmPayment(String razorpayOrderId);
    PaymentOrderResponse getPaymentByBookingId(Long bookingId);
    String getPaymentStatus(Long bookingId);
    ReceiptResponse getReceipt(Long paymentId);
    PaymentResponse retryFailedPayment(Long paymentId);
    List<PaymentHistoryResponse> getPaymentHistory();
    RefundResponse refundBooking(Long bookingId);
    //Schedular Usage - Expire Pending Payments
    void expirePendingPayments();
    void handleWebhookCallback(String rawPayload, String signature);
}