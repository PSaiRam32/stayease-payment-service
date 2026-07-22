package com.stayease.payment_service.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class ReceiptResponseDTO {
    private Long paymentId;
    private Long bookingId;

    private Long userId;

    private String receiptNumber;

    private String razorpayOrderId;

    private String transactionId;

    private Double amount;

    private Double refundAmount;

    private String currency;

    private String paymentMethod;

    private String paymentStatus;

    private LocalDateTime createdAt;

    private LocalDateTime confirmedAt;

    private LocalDateTime refundedAt;
}