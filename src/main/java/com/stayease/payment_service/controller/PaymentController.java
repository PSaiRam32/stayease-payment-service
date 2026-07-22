package com.stayease.payment_service.controller;

import com.stayease.payment_service.dto.*;
import com.stayease.payment_service.service.PaymentService;
import com.stayease.payment_service.service.RefundService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
@Tag(name = "Payment Management", description = "APIs for managing payments and orders")
public class PaymentController {

    private final PaymentService paymentService;
    private final RefundService refundService;

    // ================= CREATE ORDER =================

    @PostMapping("/order")
    @Operation(summary = "Create payment order", description = "Create a new payment order for a booking via Razorpay")
    public ResponseEntity<ApiResponse<PaymentOrderResponse>> createPaymentOrder(
            @Valid @RequestBody PaymentOrderRequest request) {
        log.info("Creating payment order for booking: {}", request.getBookingId());
        PaymentOrderResponse response = paymentService.createPaymentOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>("SUCCESS", "Payment order created", response));
    }

    // ================= CONFIRM PAYMENT =================

    @PostMapping("/confirm")
    @Operation(summary = "Confirm payment", description = "Confirm payment after customer completes Razorpay payment flow")
    public ResponseEntity<ApiResponse<PaymentResponseDTO>> confirmPayment(
            @Valid @RequestBody PaymentConfirmationRequestDTO request) {
        log.info("Confirming payment for orderId: {}", request.getRazorpayOrderId());
        PaymentResponseDTO response = paymentService.confirmPayment(request);
        return ResponseEntity.ok(
                new ApiResponse<>("SUCCESS", "Payment confirmed", response)
        );
    }

    // ================= WEBHOOK =================

//    @PostMapping("/webhook")
//    @Operation(summary = "Payment webhook", description = "Webhook endpoint for Razorpay payment callbacks")
//    public ResponseEntity<ApiResponse<String>> handleWebhook(@RequestBody WebhookPayloadDTO payload) {
//        log.info("Received webhook event: {}", payload.getEvent());
//        paymentService.handleWebhookCallback(payload);
//        return ResponseEntity.ok(
//                new ApiResponse<>("SUCCESS", "Webhook processed", "Acknowledged")
//        );
//    }

    @PostMapping("/webhook")
    @Operation(summary = "Payment webhook", description = "Webhook endpoint for Razorpay payment callbacks")
    public ResponseEntity<ApiResponse<String>> handleWebhook(@RequestBody String rawPayload, @RequestHeader("X-Razorpay-Signature") String signature) {
        paymentService.handleWebhookCallback(rawPayload, signature);
        return ResponseEntity.ok(new ApiResponse<>("SUCCESS", "Webhook processed", "Acknowledged"));
    }

    // ================= GET ORDER (DB ID) =================

    @GetMapping("/order/{id}")
    @Operation(summary = "Get order by DB ID", description = "Fetch payment order using internal database ID")
    public ResponseEntity<ApiResponse<PaymentOrderResponse>> getPaymentOrderById(
            @PathVariable Long paymentId) {
        log.info("Fetching payment order by DB id: {}", paymentId);
        PaymentOrderResponse response = paymentService.getPaymentOrderDetails(paymentId);
        return ResponseEntity.ok(
                new ApiResponse<>("SUCCESS", "Order details retrieved", response)
        );
    }

    // ================= GET ORDER (RAZORPAY ID) =================

    @GetMapping("/order/razorpay/{razorpayOrderId}")
    @Operation(summary = "Get order by Razorpay ID", description = "Fetch payment order using Razorpay order ID")
    public ResponseEntity<ApiResponse<PaymentOrderResponse>> getByRazorpayId(
            @PathVariable String razorpayOrderId) {
        log.info("Fetching payment order by Razorpay id: {}", razorpayOrderId);
        PaymentOrderResponse response =
                paymentService.getByRazorpayOrderId(razorpayOrderId);
        return ResponseEntity.ok(
                new ApiResponse<>("SUCCESS", "Order details retrieved", response)
        );
    }

    // ================= TEST PAYMENT (NO UI) =================

    @PostMapping("/test/confirm/{razorpayOrderId}")
    @Operation(summary = "Test payment confirm", description = "Simulate payment success without Razorpay UI")
    public ResponseEntity<ApiResponse<String>> testConfirmPayment(
            @PathVariable String razorpayOrderId) {
        log.warn("TEST PAYMENT TRIGGERED for orderId={}", razorpayOrderId);
        paymentService.testConfirmPayment(razorpayOrderId);
        return ResponseEntity.ok(
                new ApiResponse<>("SUCCESS", "Payment completed (TEST MODE)", "OK")
        );
    }

    // ================= REFUND APIs =================

    @PostMapping("/refund")
    @Operation(summary = "Initiate refund", description = "Initiate a refund for a confirmed payment")
    public ResponseEntity<ApiResponse<RefundResponseDTO>> initiateRefund(
            @Valid @RequestBody RefundRequestDTO request) {
        log.info("Initiating refund for payment order: {}", request.getPaymentId());
        RefundResponseDTO response = refundService.initiateRefund(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>("SUCCESS", "Refund initiated", response));
    }

    @GetMapping("/refund/{refundId}")
    @Operation(summary = "Get refund details", description = "Fetch refund details by refund ID")
    public ResponseEntity<ApiResponse<RefundResponseDTO>> getRefundDetails(
            @PathVariable Long refundId) {
        log.info("Fetching refund details: {}", refundId);
        RefundResponseDTO response = refundService.getRefundDetails(refundId);
        return ResponseEntity.ok(
                new ApiResponse<>("SUCCESS", "Refund details retrieved", response)
        );
    }

    @PostMapping("/refund/{refundId}/process")
    @Operation(summary = "Process refund", description = "Process a pending refund (admin use)")
    public ResponseEntity<ApiResponse<RefundResponseDTO>> processRefund(
            @PathVariable Long refundId) {
        log.info("Processing refund: {}", refundId);
        RefundResponseDTO response = refundService.processRefund(refundId);
        return ResponseEntity.ok(
                new ApiResponse<>("SUCCESS", "Refund processing initiated", response)
        );
    }

    @GetMapping("/history")
    public ResponseEntity<ApiResponse<List<PaymentHistoryResponse>>> getPaymentHistory(){
        List<PaymentHistoryResponse> history=paymentService.getPaymentHistory();
        return ResponseEntity.ok(
                new ApiResponse<>("SUCCESS", "Payment history fetched successfully.",history));
    }

    @PostMapping("/{paymentId}/retry")
    public ResponseEntity<ApiResponse<PaymentResponseDTO>> retryPayment(@PathVariable Long paymentId){
        PaymentResponseDTO response=paymentService.retryFailedPayment(paymentId);
        return ResponseEntity.ok(new ApiResponse<>("SUCCESS", "Payment retried successfully.", response));
    }

    @PostMapping("/refund/{refundId}/complete")
    public ResponseEntity<ApiResponse<String>> completeRefund(
            @PathVariable Long refundId) {

        refundService.completeRefund(refundId);

        return ResponseEntity.ok(
                new ApiResponse<>(
                        "SUCCESS",
                        "Refund completed successfully",
                        "OK"));
    }

    @GetMapping("/booking/{bookingId}")
    @Operation(
            summary = "Get payment by booking ID",
            description = "Fetch payment details using booking ID."
    )
    public ResponseEntity<ApiResponse<PaymentOrderResponse>> getPaymentByBookingId(
            @PathVariable Long bookingId) {

        log.info("Fetching payment for bookingId={}", bookingId);

        PaymentOrderResponse response =
                paymentService.getPaymentByBookingId(bookingId);

        return ResponseEntity.ok(
                new ApiResponse<>(
                        "SUCCESS",
                        "Payment fetched successfully",
                        response
                )
        );
    }
    @PostMapping("/booking/{bookingId}/refund")
    @Operation(
            summary = "Refund booking",
            description = "Initiate, process and complete refund for a booking."
    )
    public ResponseEntity<ApiResponse<RefundResponseDTO>> refundBooking(
            @PathVariable Long bookingId) {

        log.info("Processing refund for bookingId={}", bookingId);

        RefundResponseDTO response =
                paymentService.refundBooking(bookingId);

        return ResponseEntity.ok(
                new ApiResponse<>(
                        "SUCCESS",
                        "Refund processed successfully",
                        response
                )
        );
    }

    @GetMapping("/booking/{bookingId}/status")
    @Operation(
            summary = "Get payment status",
            description = "Fetch payment status for a booking."
    )
    public ResponseEntity<ApiResponse<String>> getPaymentStatus(
            @PathVariable Long bookingId) {

        String status = paymentService.getPaymentStatus(bookingId);

        return ResponseEntity.ok(
                new ApiResponse<>(
                        "SUCCESS",
                        "Payment status fetched successfully",
                        status
                )
        );
    }

    @GetMapping("/{paymentId}/receipt")
    public ResponseEntity<ApiResponse<ReceiptResponseDTO>> getReceipt(@PathVariable Long paymentId) {

        ReceiptResponseDTO receipt =
                paymentService.getReceipt(paymentId);

        return ResponseEntity.ok(new ApiResponse<>("success",
                "Receipt fetched successfully.",
                receipt));
    }




}