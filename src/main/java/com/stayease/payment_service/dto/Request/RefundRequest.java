package com.stayease.payment_service.dto.Request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefundRequest{
    private Long refundId;
    private Long  paymentId;
    private String razorpayOrderId;
    private Double refundAmount;
    private String currency;
    private String reason;
}

