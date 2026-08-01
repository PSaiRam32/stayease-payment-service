package com.stayease.payment_service.integrations;

import com.stayease.payment_service.config.RazorpayClient;
import com.stayease.payment_service.dto.Request.RazorpayRefundRequest;
import com.stayease.payment_service.dto.Response.RazorpayOrderResponse;
import com.stayease.payment_service.dto.Response.RazorpayPaymentResponse;
import com.stayease.payment_service.dto.Response.RazorpayRefundResponse;
import com.stayease.payment_service.exception.BusinessException;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class RazorpayServiceGateway {

    private final RazorpayClient razorpayClient;

    @Retry(name="razorpay")
    @CircuitBreaker(name="razorpay",fallbackMethod="createOrderFallback")
    @Bulkhead(name="razorpay",type=Bulkhead.Type.SEMAPHORE,fallbackMethod="createOrderFallback")
    public RazorpayOrderResponse createOrder(Map<String, Object> request){
        log.info("Calling Razorpay Create Order API");
        return razorpayClient.createOrder(request);
    }

    public RazorpayOrderResponse createOrderFallback(Map<String, Object> request,Exception ex){
        log.error("Unable to create Razorpay Order",ex);
        throw new RuntimeException("Payment Gateway is currently unavailable while creating order.",ex);
    }

    @Retry(name="razorpay")
    @CircuitBreaker(name="razorpay",fallbackMethod="getPaymentDetailsFallback")
    @Bulkhead(name="razorpay",type=Bulkhead.Type.SEMAPHORE,fallbackMethod="getPaymentDetailsFallback")
    public RazorpayPaymentResponse getPaymentDetails(String paymentId){
        log.info("Calling Razorpay Payment Details API");
        return razorpayClient.getPaymentDetails(paymentId);
    }

    public RazorpayPaymentResponse getPaymentDetailsFallback(String paymentId,Exception ex){
        log.error("Unable to fetch payment details {}",paymentId,ex);
        throw new RuntimeException(
                "Payment Gateway is currently unavailable while fetching paymentId: " + paymentId,ex);
    }

    @Retry(name="razorpay")
    @CircuitBreaker(name="razorpay",fallbackMethod="getOrderDetailsFallback")
    @Bulkhead(name="razorpay",type=Bulkhead.Type.SEMAPHORE,fallbackMethod="getOrderDetailsFallback")
    public RazorpayOrderResponse getOrderDetails(String orderId){
        log.info("Calling Razorpay Order Details API");
        return razorpayClient.getOrderDetails(orderId);
    }

    public RazorpayOrderResponse getOrderDetailsFallback(String orderId,Exception ex){
        log.error("Unable to fetch order details {}",orderId,ex);
        throw new RuntimeException(
                "Payment Gateway is currently unavailable while fetching orderId: " + orderId,ex);
    }

    @Retry(name="razorpay")
    @CircuitBreaker(name="razorpay",fallbackMethod="refundPaymentFallback")
    @Bulkhead(name="razorpay",type=Bulkhead.Type.SEMAPHORE,fallbackMethod = "refundPaymentFallback")
    public RazorpayRefundResponse refundPayment(String paymentId,RazorpayRefundRequest request){
        return razorpayClient.refundPayment(paymentId, request);

    }

    public RazorpayRefundResponse refundPaymentFallback(String paymentId,RazorpayRefundRequest request,Exception ex) {
        log.error("Refund API unavailable",ex);
        throw new RuntimeException("Payment Gateway is currently unavailable while refunding paymentId: " + paymentId,ex);
    }
}