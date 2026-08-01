package com.stayease.payment_service.config;

import com.stayease.payment_service.dto.Request.RazorpayRefundRequest;
import com.stayease.payment_service.dto.Response.RazorpayOrderResponse;
import com.stayease.payment_service.dto.Response.RazorpayPaymentResponse;
import com.stayease.payment_service.dto.Response.RazorpayRefundResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Razorpay Feign Client for Payment Gateway Integration
 * Handles order creation and payment verification
 */
@FeignClient(
        name = "razorpay-client",
        url = "https://api.razorpay.com/v1",
        configuration = RazorpayFeignConfig.class
)
public interface RazorpayClient {

    /**
     * Create a new order in Razorpay
     */
    @PostMapping("/orders")
    RazorpayOrderResponse createOrder(@RequestBody Map<String, Object> orderRequest);

    /**
     * Verify payment after completion
     */
    @GetMapping("/payments/{paymentId}")
    RazorpayPaymentResponse getPaymentDetails(@PathVariable String paymentId);

    /**
     * Fetch order details
     */
    @GetMapping("/orders/{orderId}")
    RazorpayOrderResponse getOrderDetails(@PathVariable String orderId);

    @PostMapping("/payments/{paymentId}/refund")
    RazorpayRefundResponse refundPayment(@PathVariable String paymentId,@RequestBody RazorpayRefundRequest request);
}

