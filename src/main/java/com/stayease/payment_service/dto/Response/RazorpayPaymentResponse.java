package com.stayease.payment_service.dto.Response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class RazorpayPaymentResponse{
    private String id;
    private String order_id;
    private String status;
    private Long amount;
    private String currency;
    private String method;
    private String email;
    private String contact;

}