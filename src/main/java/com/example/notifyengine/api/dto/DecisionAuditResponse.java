package com.example.notifyengine.api.dto;

import com.example.notifyengine.domain.model.Channel;
import com.example.notifyengine.domain.model.DecisionType;
import com.example.notifyengine.domain.model.EvaluatedRule;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record DecisionAuditResponse(UUID id,
                                    String eventId,
                                    String eventType,
                                    String userId,
                                    UUID ruleId,
                                    DecisionType decision,
                                    String reason,
                                    Channel channel,
                                    List<EvaluatedRule> evaluatedRules,
                                    Instant scheduledFor,
                                    Instant createdAt) {
}
