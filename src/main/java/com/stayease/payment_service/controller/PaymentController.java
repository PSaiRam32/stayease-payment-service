package com.stayease.payment_service.controller;

import com.stayease.payment_service.dto.Request.PaymentConfirmationRequest;
import com.stayease.payment_service.dto.Request.PaymentOrderRequest;
import com.stayease.payment_service.dto.Request.RefundRequest;
import com.stayease.payment_service.dto.Response.*;
import com.stayease.payment_service.service.PaymentService;
import com.stayease.payment_service.service.RefundService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    @Operation(summary="Booking Internal - Create payment order")
    public ResponseEntity<ApiResponse<PaymentOrderResponse>> createPaymentOrder(@Valid @RequestBody PaymentOrderRequest request){
        PaymentOrderResponse response=paymentService.createPaymentOrder(request);
        return ResponseEntity.ok(new ApiResponse<>("SUCCESS", "Payment order created", response));
    }

    // ================= CONFIRM PAYMENT =================

    @PostMapping("/confirm")
    @Operation(summary = "Confirm payment", description = "Confirm payment after customer completes Razorpay payment flow")
    public ResponseEntity<ApiResponse<PaymentResponse>> confirmPayment(@Valid @RequestBody PaymentConfirmationRequest request){
        PaymentResponse response=paymentService.confirmPayment(request);
        return ResponseEntity.ok(new ApiResponse<>("SUCCESS", "Payment confirmed", response));
    }

    // ================= GET ORDER (DB ID) =================

    @GetMapping("/order/{paymentId}")
    @Operation(summary = "Get order by DB ID", description = "Fetch payment order using internal database ID")
    public ResponseEntity<ApiResponse<PaymentOrderResponse>> getPaymentOrderById(@PathVariable Long paymentId){
        PaymentOrderResponse response=paymentService.getPaymentOrderDetails(paymentId);
        return ResponseEntity.ok(new ApiResponse<>("SUCCESS", "Order details retrieved", response));
    }

    // ================= GET ORDER (RAZORPAY ID) =================

    @GetMapping("/order/razorpay/{razorpayOrderId}")
    @Operation(summary = "Get order by Razorpay ID", description = "Fetch payment order using Razorpay order ID")
    public ResponseEntity<ApiResponse<PaymentOrderResponse>> getByRazorpayId(@PathVariable String razorpayOrderId){
        PaymentOrderResponse response=paymentService.getByRazorpayOrderId(razorpayOrderId);
        return ResponseEntity.ok(new ApiResponse<>("SUCCESS", "Order details retrieved", response));
    }

    // ================= TEST PAYMENT (NO UI) =================

    @PostMapping("/test/confirm/{razorpayOrderId}")
    @Operation(summary = "Test payment confirm", description = "Simulate payment success without Razorpay UI")
    public ResponseEntity<ApiResponse<String>> testConfirmPayment(@PathVariable String razorpayOrderId){
        paymentService.testConfirmPayment(razorpayOrderId);
        return ResponseEntity.ok(new ApiResponse<>("SUCCESS", "Payment completed (TEST MODE)", "OK"));
    }

    @GetMapping("/booking/{bookingId}")
    @Operation(summary = "Get payment by booking ID",description = "Fetch payment details using booking ID.")
    public ResponseEntity<ApiResponse<PaymentOrderResponse>> getPaymentByBookingId(@PathVariable Long bookingId){
        log.info("Fetching payment for bookingId={}", bookingId);
        PaymentOrderResponse response=paymentService.getPaymentByBookingId(bookingId);
        return ResponseEntity.ok(new ApiResponse<>("SUCCESS","Payment fetched successfully",response));
    }

    @GetMapping("/booking/status/{bookingId}")
    @Operation(summary = "Get payment status",description = "Fetch payment status for a booking.")
    public ResponseEntity<ApiResponse<String>> getPaymentStatus(@PathVariable Long bookingId){
        String status = paymentService.getPaymentStatus(bookingId);
        return ResponseEntity.ok(new ApiResponse<>("SUCCESS","Payment status fetched successfully",status));
    }

    @GetMapping("/{paymentId}/receipt")
    @Operation(summary="Receipt")
    public ResponseEntity<ApiResponse<ReceiptResponse>> getReceipt(@PathVariable Long paymentId){
        ReceiptResponse receipt=paymentService.getReceipt(paymentId);
        return ResponseEntity.ok(new ApiResponse<>("success","Receipt fetched successfully.",receipt));
    }


    @PostMapping("/{paymentId}/retry")
    @Operation(summary = "Retry Payment")
    public ResponseEntity<ApiResponse<PaymentResponse>> retryPayment(@PathVariable Long paymentId){
        PaymentResponse response=paymentService.retryFailedPayment(paymentId);
        return ResponseEntity.ok(new ApiResponse<>("SUCCESS", "Payment retried successfully.", response));
    }

    @GetMapping("/history")
    @Operation(summary = "Get Payment History")
    public ResponseEntity<ApiResponse<List<PaymentHistoryResponse>>> getPaymentHistory(){
        List<PaymentHistoryResponse> history=paymentService.getPaymentHistory();
        return ResponseEntity.ok(new ApiResponse<>("SUCCESS", "Payment history fetched successfully.",history));
    }


    // ================= REFUND APIs =================

    @PostMapping("/booking/{bookingId}/refund")
    @Operation(summary="Refund booking",description="Initiate, process and complete refund for a booking.")
    public ResponseEntity<ApiResponse<RefundResponse>> refundBooking(@PathVariable Long bookingId){
        log.info("Processing refund for bookingId={}", bookingId);
        RefundResponse response=paymentService.refundBooking(bookingId);
        return ResponseEntity.ok(new ApiResponse<>("SUCCESS","Refund processed successfully",response));
    }


    @PostMapping("/refund")
    @Operation(summary = "Initiate refund", description = "Initiate a refund for a confirmed payment")
    public ResponseEntity<ApiResponse<RefundResponse>> initiateRefund(
            @Valid @RequestBody RefundRequest request) {
        log.info("Initiating refund for payment order: {}", request.getPaymentId());
        RefundResponse response = refundService.initiateRefund(request);
        return ResponseEntity.ok(new ApiResponse<>("SUCCESS", "Refund initiated", response));
    }

    @GetMapping("/refund/{refundId}")
    @Operation(summary = "Get refund details", description = "Fetch refund details by refund ID")
    public ResponseEntity<ApiResponse<RefundResponse>> getRefundDetails(@PathVariable Long refundId){
        RefundResponse response=refundService.getRefundDetails(refundId);
        return ResponseEntity.ok(new ApiResponse<>("SUCCESS", "Refund details retrieved", response));
    }

    @PostMapping("/refund/{refundId}/process")
    @Operation(summary = "Process refund", description = "Process a pending refund (admin use)")
    public ResponseEntity<ApiResponse<RefundResponse>> processRefund(@PathVariable Long refundId){
        RefundResponse response = refundService.processRefund(refundId);
        return ResponseEntity.ok(new ApiResponse<>("SUCCESS", "Refund processing initiated", response));
    }


    @PostMapping("/refund/{refundId}/complete")
    public ResponseEntity<ApiResponse<String>> completeRefund(@PathVariable Long refundId){
        refundService.completeRefund(refundId);
        return ResponseEntity.ok(new ApiResponse<>("SUCCESS","Refund completed successfully","OK"));
    }

    // ================= WEBHOOK =================

    @PostMapping("/webhook")
    @Operation(summary = "Payment webhook", description = "Webhook endpoint for Razorpay payment callbacks")
    public ResponseEntity<ApiResponse<String>> handleWebhook(@RequestBody String rawPayload, @RequestHeader("X-Razorpay-Signature") String signature) {
        paymentService.handleWebhookCallback(rawPayload, signature);
        return ResponseEntity.ok(new ApiResponse<>("SUCCESS", "Webhook processed", "Acknowledged"));
    }

}