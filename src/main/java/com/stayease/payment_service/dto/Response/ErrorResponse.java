package com.stayease.payment_service.dto.Response;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class ErrorResponse{
    private String code;
    private String message;
    private LocalDateTime timestamp;
}