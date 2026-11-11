package com.example.notifyengine.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Map;

/**
 * @param payload        dados do evento (ex.: {"order": {"id": "A1", "total": 150}})
 * @param userAttributes dados do usuário (ex.: {"name": "Ana", "email": "ana@x.com"})
 */
public record EventRequest(@NotBlank @Size(max = 150) String eventId,
                           @NotBlank @Size(max = 100) String eventType,
                           @NotBlank @Size(max = 100) String userId,
                           Map<String, Object> payload,
                           Map<String, Object> userAttributes) {
}
