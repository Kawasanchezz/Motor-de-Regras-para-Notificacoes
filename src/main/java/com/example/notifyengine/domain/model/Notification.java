package com.example.notifyengine.domain.model;

import java.util.UUID;

public record Notification(UUID id,
                           String eventId,
                           String eventType,
                           String userId,
                           UUID ruleId,
                           Channel channel,
                           String recipient,
                           String message) {
}
