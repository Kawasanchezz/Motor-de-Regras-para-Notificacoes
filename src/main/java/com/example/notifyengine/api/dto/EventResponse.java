package com.example.notifyengine.api.dto;

import com.example.notifyengine.domain.model.DecisionType;

import java.util.List;

/**
 * @param decision decisão geral: SEND se alguma entrega foi enviada, senão DEFER se alguma foi adiada, senão BLOCK
 * @param replayed true quando a resposta veio do cache de idempotência (evento já processado)
 */
public record EventResponse(String eventId,
                            DecisionType decision,
                            String reason,
                            List<DeliveryResult> deliveries,
                            boolean replayed) {

    public EventResponse withReplayed(boolean value) {
        return new EventResponse(eventId, decision, reason, deliveries, value);
    }
}
