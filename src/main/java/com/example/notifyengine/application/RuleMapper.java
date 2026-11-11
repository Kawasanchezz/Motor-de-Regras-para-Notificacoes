package com.example.notifyengine.application;

import com.example.notifyengine.api.dto.ActionDto;
import com.example.notifyengine.api.dto.ConstraintDto;
import com.example.notifyengine.api.dto.RuleRequest;
import com.example.notifyengine.api.dto.RuleResponse;
import com.example.notifyengine.domain.model.ActionSpec;
import com.example.notifyengine.domain.model.ConstraintSpec;
import com.example.notifyengine.domain.model.Rule;
import com.example.notifyengine.infra.persistence.RuleActionEntity;
import com.example.notifyengine.infra.persistence.RuleConstraintEntity;
import com.example.notifyengine.infra.persistence.RuleEntity;

import java.util.HashMap;
import java.util.List;

/** Conversões entre request, entidade JPA, resposta e modelo de domínio. */
final class RuleMapper {

    private RuleMapper() {
    }

    /** Copia os dados do request para a entidade, substituindo ações e restrições. */
    static void apply(RuleEntity entity, RuleRequest request) {
        entity.setName(request.name());
        entity.setDescription(request.description());
        entity.setEventType(request.eventType());
        entity.setPriority(request.priority());
        entity.setActive(request.active() == null || request.active());
        entity.setConditionTree(request.condition());

        entity.getActions().clear();
        for (ActionDto dto : request.actions()) {
            RuleActionEntity action = new RuleActionEntity();
            action.setRule(entity);
            action.setChannel(dto.channel());
            action.setTemplate(dto.template());
            action.setRecipientExpression(dto.recipient());
            entity.getActions().add(action);
        }

        entity.getConstraints().clear();
        if (request.constraints() != null) {
            for (ConstraintDto dto : request.constraints()) {
                RuleConstraintEntity constraint = new RuleConstraintEntity();
                constraint.setRule(entity);
                constraint.setType(dto.type());
                constraint.setParams(dto.params() == null ? new HashMap<>() : new HashMap<>(dto.params()));
                entity.getConstraints().add(constraint);
            }
        }
    }

    static RuleResponse toResponse(RuleEntity e) {
        List<ActionDto> actions = e.getActions().stream()
                .map(a -> new ActionDto(a.getChannel(), a.getTemplate(), a.getRecipientExpression()))
                .toList();
        List<ConstraintDto> constraints = e.getConstraints().stream()
                .map(c -> new ConstraintDto(c.getType(), c.getParams()))
                .toList();
        return new RuleResponse(e.getId(), e.getName(), e.getDescription(), e.getEventType(), e.getPriority(),
                e.isActive(), e.getConditionTree(), actions, constraints, e.getCreatedAt(), e.getUpdatedAt());
    }

    static Rule toDomain(RuleEntity e) {
        List<ActionSpec> actions = e.getActions().stream()
                .map(a -> new ActionSpec(a.getChannel(), a.getTemplate(), a.getRecipientExpression()))
                .toList();
        List<ConstraintSpec> constraints = e.getConstraints().stream()
                .map(c -> new ConstraintSpec(c.getType(), c.getParams()))
                .toList();
        return new Rule(e.getId(), e.getName(), e.getEventType(), e.getPriority(),
                e.getConditionTree(), actions, constraints);
    }
}
