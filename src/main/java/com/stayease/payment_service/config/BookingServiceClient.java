package com.stayease.payment_service.config;

import com.stayease.payment_service.dto.Response.ApiResponse;
import com.stayease.payment_service.dto.Response.RefundResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;

/**
 * Booking Service Feign Client
 * Communicates with Booking Service for status updates
 */
@FeignClient(
        name = "booking-service",
        url = "${services.booking.url}",
        configuration = FeignClientConfig.class
)
public interface BookingServiceClient {

    @PutMapping("/bookings/{id}/confirm")
    void confirmBooking(@PathVariable("id") Long id);

    @PutMapping("/bookings/{id}/fail")
    void failBooking(@PathVariable("id") Long id);

    @PostMapping("/payments/booking/{bookingId}/refund")
    ApiResponse<RefundResponse> refundBooking(@PathVariable Long bookingId);

}

