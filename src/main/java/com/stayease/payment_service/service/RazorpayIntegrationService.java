package com.stayease.payment_service.service;

import com.stayease.payment_service.config.RazorpayClient;
import com.stayease.payment_service.dto.PaymentOrderRequestDTO;
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
public class RazorpayIntegrationService {

    private final RazorpayClient razorpayClient;

    @Value("${razorpay.key-id}")
    private String razorpayKeyId;

    @Value("${razorpay.key-secret}")
    private String razorpayKeySecret;

    /**
     * Create an order in Razorpay
     */
    public Map<String, Object> createOrder(PaymentOrderRequestDTO request) {
        log.info("Creating Razorpay order for booking: {}", request.getBookingId());

        try {
            Map<String, Object> orderRequest = new HashMap<>();
            long amountInPaise = Math.round(request.getAmount() * 100);
            orderRequest.put("amount", amountInPaise); // Amount in paise
            orderRequest.put("currency", "INR");
            orderRequest.put("receipt", "booking_" + request.getBookingId());
            orderRequest.put("payment_capture", 1); // Auto capture

            if (request.getDescription() != null) {
                orderRequest.put("description", request.getDescription());
            }

            // Add notes for reference
            Map<String, String> notes = new HashMap<>();
            notes.put("bookingId", request.getBookingId().toString());
            notes.put("paymentMethod", request.getPaymentMethod());
            orderRequest.put("notes", notes);

            Map<String, Object> response = razorpayClient.createOrder(orderRequest);
            log.info("Razorpay order created successfully: {}", response.get("id"));

            return response;
        } catch (Exception e) {
            log.error("Failed to create Razorpay order for booking: {}", request.getBookingId(), e);
            throw new BusinessException("Failed to create payment order: " + e.getMessage());
        }
    }

    /**
     * Verify payment signature from Razorpay
     */
    public boolean verifyPaymentSignature(String orderId, String paymentId, String signature) {
        log.debug("Verifying payment signature for order: {} and payment: {}", orderId, paymentId);

        try {
            String payload = orderId + "|" + paymentId;
            String generatedSignature = generateSignature(payload, razorpayKeySecret);

            boolean isValid = generatedSignature.equals(signature);

            if (!isValid) {
                log.warn("Invalid payment signature detected. Order: {}, Payment: {}", orderId, paymentId);
            } else {
                log.info("Payment signature verified successfully for order: {}", orderId);
            }

            return isValid;
        } catch (Exception e) {
            log.error("Error verifying payment signature", e);
            throw new BusinessException("Failed to verify payment signature: " + e.getMessage());
        }
    }

    /**
     * Generate HMAC SHA256 signature
     */
    private String generateSignature(String payload, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(), "HmacSHA256"));
        byte[] hash = mac.doFinal(payload.getBytes());

        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }

        return hexString.toString();
    }

    /**
     * Get payment details from Razorpay
     */
    public Map<String, Object> getPaymentDetails(String paymentId) {
        log.info("Fetching payment details for payment: {}", paymentId);

        try {
            return razorpayClient.getPaymentDetails(paymentId);
        } catch (Exception e) {
            log.error("Failed to fetch payment details for payment: {}", paymentId, e);
            throw new BusinessException("Failed to fetch payment details: " + e.getMessage());
        }
    }

    /**
     * Get order details from Razorpay
     */
    public Map<String, Object> getOrderDetails(String orderId) {
        log.info("Fetching order details for order: {}", orderId);

        try {
            return razorpayClient.getOrderDetails(orderId);
        } catch (Exception e) {
            log.error("Failed to fetch order details for order: {}", orderId, e);
            throw new BusinessException("Failed to fetch order details: " + e.getMessage());
        }
    }
}

