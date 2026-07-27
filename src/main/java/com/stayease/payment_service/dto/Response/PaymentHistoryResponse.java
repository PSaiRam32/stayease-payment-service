package com.stayease.payment_service.dto.Response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class PaymentHistoryResponse{
    private Long paymentId;
    private Long bookingId;
    private Double amount;
    private Double refundAmount;
    private String paymentStatus;
    private String paymentMethod;
    private String currency;
    private String receiptNumber;
    private String transactionId;
    private LocalDateTime createdAt;
    private LocalDateTime confirmedAt;
    private LocalDateTime refundedAt;
}