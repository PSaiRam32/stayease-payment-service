package com.stayease.payment_service.config;

import feign.RequestInterceptor;
import feign.auth.BasicAuthRequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for Razorpay Feign Client
 * Handles Basic Auth with Razorpay API credentials
 */
@Configuration
public class RazorpayFeignConfig {

    @Value("${razorpay.key-id:}")
    private String razorpayKeyId;

    @Value("${razorpay.key-secret:}")
    private String razorpayKeySecret;

    @Bean
    public RequestInterceptor basicAuthInterceptor() {
        return new BasicAuthRequestInterceptor(razorpayKeyId, razorpayKeySecret);
    }
}

