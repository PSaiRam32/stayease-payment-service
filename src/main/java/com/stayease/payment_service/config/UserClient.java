//package com.stayease.payment_service.config;
//
//import com.stayease.payment_service.dto.UserResponseDTO;
//import org.springframework.cloud.openfeign.FeignClient;
//import org.springframework.web.bind.annotation.*;
//
//@FeignClient(
//        name = "user-service",
//        url = "${services.users.url}",
//        configuration = FeignClientConfig.class
//)
//
//public interface UserClient {
//
//    @GetMapping("/users/getbookings/{bookingId}")
//    UserResponseDTO getUserByBookingId(@PathVariable Long bookingId);
//}