package com.stayease.payment_service.config;

import feign.Logger;
import feign.RequestInterceptor;
import feign.codec.ErrorDecoder;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Slf4j
@Configuration
public class FeignClientConfig {

    @Bean
    public RequestInterceptor requestInterceptor() {
        return requestTemplate -> {
            String clientName = requestTemplate.feignTarget().name();
            if ("razorpay-client".equals(clientName)) {
                log.debug("Skipping JWT for Razorpay client");
                return;
            }
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();

                String authHeader = request.getHeader("Authorization");
                String correlationId = request.getHeader("X-Correlation-Id");

                if (authHeader != null) {
                    requestTemplate.header("Authorization", authHeader);
                    log.debug("Authorization header propagated");
                }

                if (correlationId != null) {
                    requestTemplate.header("X-Correlation-Id", correlationId);
                }
            }
        };
    }

    @Bean
    public ErrorDecoder decode() {
        return (methodKey, response) -> {
            int status = response.status();
            return switch (status) {
                case 400 -> new RuntimeException("Bad Request from downstream service");
                case 404 -> new RuntimeException("Resource not found in downstream service");
                case 409 -> new RuntimeException("Conflict in downstream service");
                case 500 -> new RuntimeException("Internal server error in downstream service");
                default -> new RuntimeException("Feign client error: " + status);
            };
        };
    }

    @Bean
    Logger.Level feignLoggerLevel() {
        return Logger.Level.FULL;
    }
}