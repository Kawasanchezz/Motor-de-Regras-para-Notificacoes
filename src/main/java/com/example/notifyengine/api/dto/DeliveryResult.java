package com.example.notifyengine.api.dto;

import com.example.notifyengine.domain.model.Channel;
import com.example.notifyengine.domain.model.DecisionType;

import java.time.Instant;
import java.util.UUID;

/** Decisão para uma ação (canal) de uma regra que casou com o evento. */
public record DeliveryResult(UUID ruleId,
                             Channel channel,
                             DecisionType decision,
                             String reason,
                             Instant scheduledFor) {
}
