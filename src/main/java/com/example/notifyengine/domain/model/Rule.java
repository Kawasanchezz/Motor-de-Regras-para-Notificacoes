package com.example.notifyengine.domain.model;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Regra pronta para ser avaliada pelo motor (visão de domínio, sem JPA). */
public record Rule(UUID id,
                   String name,
                   String eventType,
                   int priority,
                   ConditionNode condition,
                   List<ActionSpec> actions,
                   List<ConstraintSpec> constraints) {

    public Rule {
        actions = actions == null ? List.of() : actions;
        constraints = constraints == null ? List.of() : constraints;
    }

    public Optional<ConstraintSpec> constraint(ConstraintType type) {
        return constraints.stream().filter(c -> c.type() == type).findFirst();
    }
}
