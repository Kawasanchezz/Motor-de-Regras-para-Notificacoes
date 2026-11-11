package com.example.notifyengine.application;

import com.example.notifyengine.api.dto.DecisionAuditResponse;
import com.example.notifyengine.domain.model.DecisionType;
import com.example.notifyengine.domain.model.Notification;
import com.example.notifyengine.infra.persistence.DecisionAudit;
import com.example.notifyengine.infra.persistence.DecisionAuditRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class AuditService {

    private final DecisionAuditRepository repository;

    public AuditService(DecisionAuditRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void save(DecisionAudit audit) {
        repository.save(audit);
    }

    /** Registra o despacho de uma notificação que estava adiada na fila. */
    @Transactional
    public void recordDeferredDispatch(Notification notification, DecisionType decision, String reason) {
        repository.save(DecisionAudit.builder()
                .eventId(notification.eventId())
                .eventType(notification.eventType())
                .userId(notification.userId())
                .ruleId(notification.ruleId())
                .decision(decision)
                .reason(reason)
                .channel(notification.channel())
                .build());
    }

    @Transactional(readOnly = true)
    public Page<DecisionAuditResponse> search(String userId,
                                              String eventType,
                                              DecisionType decision,
                                              Instant from,
                                              Instant to,
                                              Pageable pageable) {
        Specification<DecisionAudit> spec = (root, query, cb) -> cb.conjunction();
        if (userId != null && !userId.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("userId"), userId));
        }
        if (eventType != null && !eventType.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("eventType"), eventType));
        }
        if (decision != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("decision"), decision));
        }
        if (from != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.<Instant>get("createdAt"), from));
        }
        if (to != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.<Instant>get("createdAt"), to));
        }
        return repository.findAll(spec, pageable).map(AuditService::toResponse);
    }

    private static DecisionAuditResponse toResponse(DecisionAudit a) {
        return new DecisionAuditResponse(a.getId(), a.getEventId(), a.getEventType(), a.getUserId(), a.getRuleId(),
                a.getDecision(), a.getReason(), a.getChannel(), a.getEvaluatedRules(),
                a.getScheduledFor(), a.getCreatedAt());
    }
}
