package com.stayease.payment_service.service;

public interface RazorpayVerificationService {

    boolean verifyPaymentSignature(String orderId,String paymentId,String signature);
    boolean verifyWebhookSignature(String payload, String signature);
}
