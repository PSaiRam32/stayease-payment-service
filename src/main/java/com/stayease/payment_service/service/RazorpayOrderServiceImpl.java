package com.stayease.payment_service.service;

import com.stayease.payment_service.dto.Request.PaymentOrderRequest;
import com.stayease.payment_service.dto.Request.RazorpayRefundRequest;
import com.stayease.payment_service.dto.Response.RazorpayOrderResponse;
import com.stayease.payment_service.dto.Response.RazorpayPaymentResponse;
import com.stayease.payment_service.dto.Response.RazorpayRefundResponse;
import com.stayease.payment_service.exception.BusinessException;
import com.stayease.payment_service.integrations.RazorpayServiceGateway;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
@AllArgsConstructor
public class RazorpayOrderServiceImpl implements RazorpayOrderService {

    private final RazorpayServiceGateway razorpayServiceGateway;

    @Override
    public RazorpayOrderResponse createOrder(PaymentOrderRequest request){
        log.info("Creating Razorpay order for booking: {}", request.getBookingId());
        try {
            Map<String, Object> orderRequest=new HashMap<>();
            long amountInPaise=Math.round(request.getAmount() * 100);
            orderRequest.put("amount",amountInPaise); // Amount in paise
            orderRequest.put("currency","INR");
            orderRequest.put("receipt",request.getDescription() != null
                    ? request.getDescription()
                    : "booking_" + request.getBookingId());
            orderRequest.put("payment_capture", 1); // Auto capture
            orderRequest.put("description",request.getDescription() != null
                    ? request.getDescription()
                    : "Payment for booking " + request.getBookingId());
            Map<String, String> notes=new HashMap<>();
            notes.put("bookingId",request.getBookingId().toString());
            notes.put("paymentMethod",request.getPaymentMethod());
            notes.put("email",request.getCustomerEmail() != null
                    ? request.getCustomerEmail()
                    : "unknown");
            notes.put("phone",request.getCustomerPhone() != null
                    ? request.getCustomerPhone()
                    : "unknown");
            orderRequest.put("notes",notes);
            RazorpayOrderResponse response=razorpayServiceGateway.createOrder(orderRequest);
            log.info("Razorpay order created successfully: {}",response.getId());
            return response;
        }
        catch (Exception e){
            log.error("Failed to create Razorpay order for booking: {}",request.getBookingId(), e);
            throw new BusinessException("Failed to create payment order: " + e.getMessage());
        }
    }

    @Override
    public RazorpayPaymentResponse getPaymentDetails(String paymentId){
        log.info("Fetching payment details for payment: {}",paymentId);
        try {
            return razorpayServiceGateway.getPaymentDetails(paymentId);
        } catch (Exception e) {
            log.error("Failed to fetch payment details for payment: {}",paymentId, e);
            throw new BusinessException("Failed to fetch payment details: " + e.getMessage());
        }
    }

    @Override
    public RazorpayOrderResponse getOrderDetails(String orderId){
        log.info("Fetching order details for order: {}",orderId);
        try {
            return razorpayServiceGateway.getOrderDetails(orderId);
        } catch (Exception e) {
            log.error("Failed to fetch order details for order: {}",orderId, e);
            throw new BusinessException("Failed to fetch order details: " + e.getMessage());
        }
    }

    @Override
    public RazorpayRefundResponse refundPayment(String paymentId,Double amount){
        RazorpayRefundRequest request=RazorpayRefundRequest.builder()
                        .amount(Math.round(amount * 100))
                        .notes("StayEase Refund")
                        .build();
        return razorpayServiceGateway.refundPayment(paymentId,request);
    }

}
