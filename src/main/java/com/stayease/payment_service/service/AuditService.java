package com.stayease.payment_service.service;

import com.stayease.payment_service.entity.AuditLog;
import com.stayease.payment_service.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;


@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private static final String CORRELATION_ID_HEADER = "X-Correlation-Id";

//    /**
//     * Log an event to audit log
//     */
//    public void logEvent(String razorpayOrderId, Long paymentOrderId, String eventType, String description) {
//        logEvent(razorpayOrderId,paymentOrderId, eventType, description, null, null);
//    }

    public void logEvent( String razorpayOrderId,Long paymentId, String eventType, String description) {
        try {
            String correlationId = getCorrelationId();
            AuditLog auditLog = AuditLog.builder()
                    .razorpayOrderId(razorpayOrderId)
                    .paymentId(paymentId)
                    .eventType(eventType)
                    .description(description)
                    .correlationId(correlationId)
                    .createdAt(LocalDateTime.now())
                    .build();
            auditLogRepository.save(auditLog);
            log.debug("Audit log created - Event: {}, OrderId: {}, CorrelationId: {}",
                    eventType, razorpayOrderId, correlationId);
        } catch (Exception e) {
            log.error("Failed to create audit log for payment order: {}", razorpayOrderId, e);
            // Don't throw - audit logging should not block business operations
        }
    }

    private String getCorrelationId() {
        try {
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                Object correlationId = attributes.getRequest().getAttribute(CORRELATION_ID_HEADER);
                if (correlationId != null) {
                    return correlationId.toString();
                }
            }
        } catch (Exception e) {
            log.debug("Could not retrieve correlation ID from request context", e);
        }
        return "UNKNOWN";
    }
}

