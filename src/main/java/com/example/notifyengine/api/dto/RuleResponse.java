package com.example.notifyengine.api.dto;

import com.example.notifyengine.domain.model.ConditionNode;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record RuleResponse(UUID id,
                           String name,
                           String description,
                           String eventType,
                           int priority,
                           boolean active,
                           ConditionNode condition,
                           List<ActionDto> actions,
                           List<ConstraintDto> constraints,
                           Instant createdAt,
                           Instant updatedAt) {
}
