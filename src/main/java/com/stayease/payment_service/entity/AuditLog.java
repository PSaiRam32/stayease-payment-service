package com.stayease.payment_service.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Entity for auditing payment-related events and state changes
 * Used for compliance, debugging, and transaction history
 */
@Entity
@Table(name = "payment_audit_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long paymentId;

    @Column(nullable = false)
    private String razorpayOrderId;

    @Column(nullable = false)
    private String eventType; // e.g., ORDER_CREATED, PAYMENT_INITIATED, SIGNATURE_VERIFIED, BOOKING_UPDATED

    @Column(nullable = false, length = 500)
    private String description;

    private String correlationId;

    @Column(columnDefinition = "LONGTEXT")
    private String requestPayload;

    @Column(columnDefinition = "LONGTEXT")
    private String responsePayload;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}

