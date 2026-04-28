package com.stayease.payment_service.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "payment_orders", uniqueConstraints = {
        @UniqueConstraint(columnNames = "bookingId", name = "uk_payment_order_booking_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long paymentId;

    @Column(nullable = false)
    private Long bookingId;

    @Column(nullable = false)
    private Double amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentOrderStatus status;

    @Column(nullable = false)
    private String paymentMethod;

    private String razorpayOrderId;
    private String transactionId;
    private String errorMessage;

    @Column(nullable = false)
    private Integer attemptCount;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime confirmedAt;
    private LocalDateTime expiredAt;

    @Version
    private Long version; // For optimistic locking - idempotency

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.attemptCount = 0;
    }
}

