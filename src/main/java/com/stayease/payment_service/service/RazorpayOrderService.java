package com.stayease.payment_service.service;

import com.stayease.payment_service.dto.Request.PaymentOrderRequest;
import com.stayease.payment_service.dto.Response.RazorpayOrderResponse;
import com.stayease.payment_service.dto.Response.RazorpayPaymentResponse;
import com.stayease.payment_service.dto.Response.RazorpayRefundResponse;

import java.util.Map;

public interface RazorpayOrderService {

    RazorpayOrderResponse createOrder(PaymentOrderRequest request);
    RazorpayPaymentResponse getPaymentDetails(String paymentId);
    RazorpayOrderResponse getOrderDetails(String orderId);
    RazorpayRefundResponse refundPayment(String paymentId,Double amount);

}
