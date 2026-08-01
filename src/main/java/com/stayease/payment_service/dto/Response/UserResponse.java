package com.stayease.payment_service.dto.Response;

import lombok.Data;

@Data
public class UserResponse {

    private Long userId;
    private String name;
    private String email;
    private String role;
    private String phone;
}
