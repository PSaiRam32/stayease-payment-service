package com.stayease.payment_service.service;

import com.stayease.payment_service.config.RazorpayClient;
import com.stayease.payment_service.dto.Request.PaymentOrderRequest;
import com.stayease.payment_service.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class RazorpayIntegrationService{

    private final RazorpayClient razorpayClient;

    @Value("${razorpay.key-id}")
    private String razorpayKeyId;

    @Value("${razorpay.key-secret}")
    private String razorpayKeySecret;


    public Map<String, Object> createOrder(PaymentOrderRequest request){
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
            Map<String, Object> response=razorpayClient.createOrder(orderRequest);
            log.info("Razorpay order created successfully: {}",response.get("id"));
            return response;
        }
        catch (Exception e){
            log.error("Failed to create Razorpay order for booking: {}",request.getBookingId(), e);
            throw new BusinessException("Failed to create payment order: " + e.getMessage());
        }
    }

    public boolean verifyPaymentSignature(String orderId,String paymentId,String signature){
        log.debug("Verifying payment signature for order: {} and payment: {}",orderId,paymentId);
        try {
            String payload=orderId + "|" + paymentId;
            String generatedSignature=generateSignature(payload,razorpayKeySecret);
            boolean isValid=generatedSignature.equals(signature);
            if (!isValid){
                log.warn("Invalid payment signature detected. Order: {}, Payment: {}",orderId,paymentId);
            } else {
                log.info("Payment signature verified successfully for order: {}",orderId);
            }
            return isValid;
        } catch (Exception e){
            log.error("Error verifying payment signature",e);
            throw new BusinessException("Failed to verify payment signature: " + e.getMessage());
        }
    }

    private String generateSignature(String payload, String secret) throws Exception{
        Mac mac=Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(), "HmacSHA256"));
        byte[] hash=mac.doFinal(payload.getBytes());
        StringBuilder hexString=new StringBuilder();
        for (byte b : hash){
            String hex=Integer.toHexString(0xff & b);
            if (hex.length()==1){
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    public Map<String, Object> getPaymentDetails(String paymentId){
        log.info("Fetching payment details for payment: {}",paymentId);
        try {
            return razorpayClient.getPaymentDetails(paymentId);
        } catch (Exception e) {
            log.error("Failed to fetch payment details for payment: {}",paymentId, e);
            throw new BusinessException("Failed to fetch payment details: " + e.getMessage());
        }
    }

    public Map<String, Object> getOrderDetails(String orderId){
        log.info("Fetching order details for order: {}",orderId);
        try {
            return razorpayClient.getOrderDetails(orderId);
        } catch (Exception e) {
            log.error("Failed to fetch order details for order: {}",orderId, e);
            throw new BusinessException("Failed to fetch order details: " + e.getMessage());
        }
    }

    public boolean verifyWebhookSignature(String payload, String signature){
        try {
            String secret="razorpayWebhookSecret"; // inject from YAML
            String generatedSignature=hmacSHA256(payload, secret);
            return generatedSignature.equals(signature);
        } catch (Exception e) {
            return false;
        }
    }

    private String hmacSHA256(String data, String secret) throws Exception{
        javax.crypto.Mac mac=javax.crypto.Mac.getInstance("HmacSHA256");
        javax.crypto.spec.SecretKeySpec secretKey=new javax.crypto.spec.SecretKeySpec(secret.getBytes(), "HmacSHA256");
        mac.init(secretKey);
        byte[] rawHmac=mac.doFinal(data.getBytes());
        return java.util.Base64.getEncoder().encodeToString(rawHmac);
    }
}

