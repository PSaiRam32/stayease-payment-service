package com.stayease.payment_service.repository;

import com.stayease.payment_service.entity.RefundTransaction;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface RefundTransactionRepository extends JpaRepository<RefundTransaction, Long> {

//    Optional<RefundTransaction> findByRefundId(Long refundId);
//    List<RefundTransaction> findByPaymentOrderId(String razorpayOrderId);
//    List<RefundTransaction> findByStatus(String status);
    boolean existsByrazorpayOrderId(String razorpayOrderId);
//    List<RefundTransaction> findByCreatedAtBetween(LocalDateTime startTime, LocalDateTime endTime);
}

