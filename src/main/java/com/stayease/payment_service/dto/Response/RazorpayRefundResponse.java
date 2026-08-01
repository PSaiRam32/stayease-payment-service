package com.stayease.payment_service.dto.Response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class RazorpayRefundResponse{
    private String id;
    private Long amount;
    private String currency;
    private String payment_id;
    private String status;
    private String speed;
    private String receipt;

}