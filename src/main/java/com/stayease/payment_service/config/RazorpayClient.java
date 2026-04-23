package com.stayease.payment_service.config;

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
    Map<String, Object> createOrder(@RequestBody Map<String, Object> orderRequest);

    /**
     * Verify payment after completion
     */
    @GetMapping("/payments/{paymentId}")
    Map<String, Object> getPaymentDetails(@PathVariable String paymentId);

    /**
     * Fetch order details
     */
    @GetMapping("/orders/{orderId}")
    Map<String, Object> getOrderDetails(@PathVariable String orderId);
}

