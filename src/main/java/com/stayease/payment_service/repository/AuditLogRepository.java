package com.stayease.payment_service.repository;

import com.stayease.payment_service.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
//    List<AuditLog> findByRazorpayOrderId(String razorpayOrderId);
//    List<AuditLog> findByCorrelationId(String correlationId);
//    List<AuditLog> findByEventTypeAndCreatedAtBetween(String eventType, LocalDateTime startTime, LocalDateTime endTime);
}
