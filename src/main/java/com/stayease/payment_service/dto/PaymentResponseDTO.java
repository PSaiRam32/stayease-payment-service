package com.stayease.payment_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponseDTO {
    private String razorpayOrderId;
    private Long paymentId;
    private Long bookingId;
    private Double amount;
    private String paymentStatus;
    private String transactionId;
}

