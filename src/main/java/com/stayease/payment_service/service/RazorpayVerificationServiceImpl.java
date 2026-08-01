package com.stayease.payment_service.service;

import com.stayease.payment_service.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

@Service
@Slf4j

public class RazorpayVerificationServiceImpl implements RazorpayVerificationService {

//    @Value("${razorpay.key-id}")
//    private String razorpayKeyId;

    @Value("${razorpay.key-secret}")
    private String razorpayKeySecret;

    @Value("${razorpay.webhook-secret}")
    private String webhookSecret;

    @Override
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


    @Override
    public boolean verifyWebhookSignature(String payload, String signature){
        try {
            String generatedSignature=hmacSHA256(payload, webhookSecret);
            return generatedSignature.equals(signature);
        }
        catch(Exception e){
            log.error("Webhook signature verification failed", e);
            throw new BusinessException("Webhook signature verification failed.");
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
