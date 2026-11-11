package com.example.notifyengine.application;

import com.example.notifyengine.api.dto.DeliveryResult;
import com.example.notifyengine.api.dto.EventRequest;
import com.example.notifyengine.api.dto.EventResponse;
import com.example.notifyengine.config.EngineProperties;
import com.example.notifyengine.domain.engine.RuleEngine;
import com.example.notifyengine.domain.engine.TemplateRenderer;
import com.example.notifyengine.domain.engine.constraint.ConstraintContext;
import com.example.notifyengine.domain.model.ActionSpec;
import com.example.notifyengine.domain.model.Decision;
import com.example.notifyengine.domain.model.DecisionType;
import com.example.notifyengine.domain.model.EvaluatedRule;
import com.example.notifyengine.domain.model.EvaluationContext;
import com.example.notifyengine.domain.model.Notification;
import com.example.notifyengine.domain.model.Rule;
import com.example.notifyengine.domain.port.NotificationScheduler;
import com.example.notifyengine.infra.channel.NotificationDispatcher;
import com.example.notifyengine.infra.persistence.DecisionAudit;
import com.example.notifyengine.infra.redis.RuleCache;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Orquestra o fluxo de um evento: idempotência, carga de regras (cache), avaliação de condições,
 * restrições, despacho/agendamento e auditoria.
 */
@Service
public class EventProcessingService {

    private static final Logger log = LoggerFactory.getLogger(EventProcessingService.class);

    private final RuleCache ruleCache;
    private final RuleEngine engine;
    private final NotificationDispatcher dispatcher;
    private final NotificationScheduler scheduler;
    private final AuditService audit;
    private final IdempotencyService idempotency;
    private final EngineProperties properties;
    private final MeterRegistry meters;
    private final Clock clock;

    public EventProcessingService(RuleCache ruleCache,
                                  RuleEngine engine,
                                  NotificationDispatcher dispatcher,
                                  NotificationScheduler scheduler,
                                  AuditService audit,
                                  IdempotencyService idempotency,
                                  EngineProperties properties,
                                  MeterRegistry meters,
                                  Clock clock) {
        this.ruleCache = ruleCache;
        this.engine = engine;
        this.dispatcher = dispatcher;
        this.scheduler = scheduler;
        this.audit = audit;
        this.idempotency = idempotency;
        this.properties = properties;
        this.meters = meters;
        this.clock = clock;
    }

    public EventResponse process(EventRequest request, String idempotencyKey) {
        String key = (idempotencyKey == null || idempotencyKey.isBlank()) ? request.eventId() : idempotencyKey;
        return idempotency.execute(key, () -> evaluate(request, key));
    }

    private EventResponse evaluate(EventRequest request, String idempotencyKey) {
        Instant now = clock.instant();
        EvaluationContext context = new EvaluationContext(request.eventId(), request.eventType(),
                request.userId(), request.payload(), request.userAttributes());

        List<Rule> rules = ruleCache.getActiveRules(request.eventType());
        List<EvaluatedRule> evaluated = new ArrayList<>();
        List<DeliveryResult> deliveries = new ArrayList<>();

        for (Rule rule : rules) {
            boolean matched = engine.matches(rule, context);
            evaluated.add(new EvaluatedRule(rule.id(), matched));
            if (!matched) {
                continue;
            }
            List<DeliveryResult> ruleResults = new ArrayList<>();
            for (ActionSpec action : rule.actions()) {
                ruleResults.add(handleAction(rule, action, context, now));
            }
            deliveries.addAll(ruleResults);

            boolean anyDelivered = ruleResults.stream().anyMatch(r -> r.decision() != DecisionType.BLOCK);
            if (anyDelivered && properties.stopOnFirstMatch()) {
                break;
            }
        }

        persistAudit(request, idempotencyKey, rules, evaluated, deliveries);
        return buildResponse(request, rules, deliveries);
    }

    private DeliveryResult handleAction(Rule rule, ActionSpec action, EvaluationContext event, Instant now) {
        ConstraintContext constraintContext = new ConstraintContext(rule, action, event, now);
        Decision decision = engine.checkConstraints(constraintContext);

        Notification notification = new Notification(UUID.randomUUID(), event.eventId(), event.eventType(),
                event.userId(), rule.id(), action.channel(),
                TemplateRenderer.resolveRecipient(action.recipient(), event),
                TemplateRenderer.render(action.template(), event));

        switch (decision.type()) {
            case SEND -> {
                if (dispatcher.dispatch(notification)) {
                    engine.commit(constraintContext);
                    decision = Decision.send("NOTIFICATION_SENT");
                } else {
                    decision = Decision.block("DISPATCH_FAILED");
                }
            }
            case DEFER -> {
                scheduler.schedule(notification, decision.scheduledFor());
                engine.commit(constraintContext);
                log.debug("Notificação {} agendada para {}", notification.id(), decision.scheduledFor());
            }
            case BLOCK -> log.debug("Notificação bloqueada pela regra {}: {}", rule.id(), decision.reason());
        }

        meters.counter("notify.decisions",
                "decision", decision.type().name(),
                "channel", action.channel().name()).increment();
        return new DeliveryResult(rule.id(), action.channel(), decision.type(), decision.reason(),
                decision.scheduledFor());
    }

    private void persistAudit(EventRequest request,
                              String idempotencyKey,
                              List<Rule> rules,
                              List<EvaluatedRule> evaluated,
                              List<DeliveryResult> deliveries) {
        if (deliveries.isEmpty()) {
            audit.save(baseAudit(request, idempotencyKey, evaluated)
                    .decision(DecisionType.BLOCK)
                    .reason(rules.isEmpty() ? "NO_ACTIVE_RULES" : "NO_MATCHING_RULE")
                    .build());
            return;
        }
        for (DeliveryResult delivery : deliveries) {
            audit.save(baseAudit(request, idempotencyKey, evaluated)
                    .ruleId(delivery.ruleId())
                    .decision(delivery.decision())
                    .reason(delivery.reason())
                    .channel(delivery.channel())
                    .scheduledFor(delivery.scheduledFor())
                    .build());
        }
    }

    private DecisionAudit.DecisionAuditBuilder baseAudit(EventRequest request,
                                                         String idempotencyKey,
                                                         List<EvaluatedRule> evaluated) {
        return DecisionAudit.builder()
                .eventId(request.eventId())
                .idempotencyKey(idempotencyKey)
                .eventType(request.eventType())
                .userId(request.userId())
                .payload(request.payload())
                .evaluatedRules(evaluated);
    }

    private EventResponse buildResponse(EventRequest request, List<Rule> rules, List<DeliveryResult> deliveries) {
        if (deliveries.isEmpty()) {
            return new EventResponse(request.eventId(), DecisionType.BLOCK,
                    rules.isEmpty() ? "NO_ACTIVE_RULES" : "NO_MATCHING_RULE", deliveries, false);
        }
        DeliveryResult representative = pick(deliveries, DecisionType.SEND);
        if (representative == null) {
            representative = pick(deliveries, DecisionType.DEFER);
        }
        if (representative == null) {
            representative = deliveries.get(0);
        }
        return new EventResponse(request.eventId(), representative.decision(), representative.reason(),
                deliveries, false);
    }

    private static DeliveryResult pick(List<DeliveryResult> deliveries, DecisionType type) {
        return deliveries.stream().filter(d -> d.decision() == type).findFirst().orElse(null);
    }
}
