package com.stayease.payment_service.dto.Response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WebhookPayload{
    private String event;
    private Map<String, Object> payload;
}

