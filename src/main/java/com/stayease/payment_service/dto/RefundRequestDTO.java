package com.stayease.payment_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefundRequestDTO {
    private Long refundId;
    private Long  paymentId;
    private String razorpayOrderId;
    private Double refundAmount;
    private String currency;
    private String reason;
}

