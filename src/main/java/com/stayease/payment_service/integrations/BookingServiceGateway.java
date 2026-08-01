package com.stayease.payment_service.integrations;


import com.stayease.payment_service.config.BookingServiceClient;
import com.stayease.payment_service.dto.Response.ApiResponse;
import com.stayease.payment_service.dto.Response.RefundResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingServiceGateway{

    private final BookingServiceClient bookingServiceClient;

    @Retry(name="booking-service")
    @CircuitBreaker(name="booking-service",fallbackMethod="confirmBookingFallback")
    public void confirmBooking(Long bookingId){
        log.info("Calling Booking Service to confirm booking: {}",bookingId);
        bookingServiceClient.confirmBooking(bookingId);
    }

    public void confirmBookingFallback(Long bookingId,Exception ex){
        log.error("Unable to confirm booking {}",bookingId,ex);
        throw new RuntimeException(
                "Booking Service is currently unavailable while confirming bookingId: " + bookingId,ex);
    }

    @Retry(name="booking-service")
    @CircuitBreaker(name="booking-service",fallbackMethod="failBookingFallback")
    public void failBooking(Long bookingId){
        log.info("Calling Booking Service to fail booking: {}",bookingId);
        bookingServiceClient.failBooking(bookingId);
    }

    public void failBookingFallback(Long bookingId,Exception ex){
        log.error("Unable to fail booking {}",bookingId,ex);
        throw new RuntimeException(
                "Booking Service is currently unavailable while failing bookingId: " + bookingId,ex);
    }

    @Retry(name="booking-service")
    @CircuitBreaker(name="booking-service",fallbackMethod="refundBookingFallback")
    public ApiResponse<RefundResponse> refundBooking(Long bookingId){
        log.info("Calling Booking Service for refund sync: {}",bookingId);
        return bookingServiceClient.refundBooking(bookingId);
    }

    public ApiResponse<RefundResponse> refundBookingFallback(Long bookingId,Exception ex){
        log.error("Unable to sync refund {}",bookingId,ex);
        throw new RuntimeException(
                "Booking Service is currently unavailable while synchronizing refund for bookingId: " + bookingId,ex);
    }

}