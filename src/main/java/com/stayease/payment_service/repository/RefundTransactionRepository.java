package com.stayease.payment_service.repository;

import com.stayease.payment_service.entity.RefundTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface RefundTransactionRepository extends JpaRepository<RefundTransaction, Long> {

    Optional<RefundTransaction> findByRefundId(String refundId);
    List<RefundTransaction> findByPaymentOrderId(Long paymentOrderId);
    List<RefundTransaction> findByStatus(String status);
    boolean existsByPaymentOrderIdAndStatus(Long paymentOrderId, String status);
    List<RefundTransaction> findByCreatedAtBetween(LocalDateTime startTime, LocalDateTime endTime);
}

