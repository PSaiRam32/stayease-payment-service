package com.stayease.payment_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class PaymentOrderResponseDTO {

    private Long id;
    private Long bookingId;
    private Double amount;
    private String razorpayOrderId;
    private String status;
    private String paymentMethod;
    private LocalDateTime createdAt;
    private LocalDateTime confirmedAt;
}
