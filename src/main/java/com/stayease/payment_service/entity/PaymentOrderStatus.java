package com.stayease.payment_service.entity;

public enum PaymentOrderStatus{
    PENDING,           // Order created, awaiting payment
    PAYMENT_INITIATED, // Payment gateway initiated
    PAYMENT_CONFIRMED, // Payment gateway confirmed
    PAYMENT_FAILED,    // Payment failed
    EXPIRED,           // Order expired
    CANCELLED,         // Order cancelled
    REFUND_PENDING,
    REFUNDED
}

