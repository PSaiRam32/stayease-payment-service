package com.stayease.payment_service.dto.Response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class RazorpayOrderResponse{
    private String id;
    private Long amount;
    private String currency;
    private String receipt;
    private String status;
}