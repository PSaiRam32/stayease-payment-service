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

    /**
     * Log an event to audit log
     */
    public void logEvent(Long paymentOrderId, String eventType, String description) {
        logEvent(paymentOrderId, eventType, description, null, null);
    }

    /**
     * Log an event with payload details
     */
    public void logEvent(Long paymentOrderId, String eventType, String description,
                        String requestPayload, String responsePayload) {
        try {
            String correlationId = getCorrelationId();

            AuditLog auditLog = AuditLog.builder()
                    .paymentOrderId(paymentOrderId)
                    .eventType(eventType)
                    .description(description)
                    .correlationId(correlationId)
                    .requestPayload(requestPayload)
                    .responsePayload(responsePayload)
                    .createdAt(LocalDateTime.now())
                    .build();

            auditLogRepository.save(auditLog);
            log.debug("Audit log created - Event: {}, OrderId: {}, CorrelationId: {}",
                    eventType, paymentOrderId, correlationId);
        } catch (Exception e) {
            log.error("Failed to create audit log for payment order: {}", paymentOrderId, e);
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

