package com.example.notifyengine.domain.model;

import java.util.UUID;

/** Registro de uma regra avaliada para um evento (usado na auditoria). */
public record EvaluatedRule(UUID ruleId, boolean matched) {
}
