package com.stayease.payment_service.dto.Request;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RazorpayRefundRequest{
    private Long amount;
    private String notes;
}