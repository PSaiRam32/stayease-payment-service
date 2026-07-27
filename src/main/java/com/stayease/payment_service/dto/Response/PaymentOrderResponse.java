package com.stayease.payment_service.dto.Response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class PaymentOrderResponse{
    private Long paymentId;
    private Long bookingId;
    private Double amount;
    private String razorpayOrderId;
    private String status;
    private String paymentMethod;
    private LocalDateTime createdAt;
    private LocalDateTime confirmedAt;
    private Long userId;
    private String currency;
    private String receiptNumber;
    private Double refundAmount;
    private LocalDateTime refundedAt;
}
