package com.stayease.payment_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entity representing a refund transaction
 * Tracks all refund requests and their status
 */
@Entity
@Table(name = "refund_transactions", indexes = {
        @Index(name = "idx_payment_order_id", columnList = "paymentOrderId"),
        @Index(name = "idx_refund_id", columnList = "refundId"),
        @Index(name = "idx_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefundTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long refundId;

    private Long paymentId;

    @Column(nullable = false)
    private String razorpayOrderId;

    @Column(nullable = false)
    private Double amount;

    @Column(nullable = false)
    private String currency;

    @Column(length = 500)
    private String reason;

    @Column(nullable = false)
    private String status; // INITIATED, PROCESSING, COMPLETED, FAILED

    private String externalRefundId; // Reference from payment gateway

    private String failureReason;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime processedAt;

    private LocalDateTime completedAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}

