package com.stayease.payment_service.repository;

import com.stayease.payment_service.entity.PaymentOrder;
import com.stayease.payment_service.entity.PaymentOrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.List;

@Repository
public interface PaymentOrderRepository extends JpaRepository<PaymentOrder, Long> {

    Optional<PaymentOrder> findByBookingId(Long bookingId);
    Optional<PaymentOrder> findByRazorpayOrderId(String razorpayOrderId);
    List<PaymentOrder> findByStatus(PaymentOrderStatus status);
    @Query("SELECT p FROM PaymentOrder p WHERE p.status = 'PENDING' AND p.expiredAt < :currentTime")
    List<PaymentOrder> findExpiredOrders(@Param("currentTime") LocalDateTime currentTime);
    @Query("SELECT COUNT(p) > 0 FROM PaymentOrder p WHERE p.bookingId = :bookingId AND p.status = :status")
    boolean existsByBookingIdAndStatus(@Param("bookingId") Long bookingId, @Param("status") PaymentOrderStatus status);
    List<PaymentOrder> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<PaymentOrder> findByStatusAndExpiredAtBefore(
            PaymentOrderStatus status,
            LocalDateTime now);

}

