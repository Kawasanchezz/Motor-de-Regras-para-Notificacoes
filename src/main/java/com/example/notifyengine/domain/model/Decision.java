package com.example.notifyengine.domain.model;

import java.time.Instant;

/**
 * Resultado de uma avaliação: enviar, adiar (com horário) ou bloquear (com motivo).
 */
public record Decision(DecisionType type, String reason, Instant scheduledFor) {

    public static Decision send(String reason) {
        return new Decision(DecisionType.SEND, reason, null);
    }

    public static Decision block(String reason) {
        return new Decision(DecisionType.BLOCK, reason, null);
    }

    public static Decision defer(String reason, Instant scheduledFor) {
        return new Decision(DecisionType.DEFER, reason, scheduledFor);
    }
}
