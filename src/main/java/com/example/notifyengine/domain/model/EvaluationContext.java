package com.example.notifyengine.domain.model;

import java.util.Map;

/**
 * Dados do evento disponíveis para condições, templates e destinatários.
 * <p>
 * Resolução de caminhos: {@code user.x} lê de {@code userAttributes};
 * {@code event.x} ou apenas {@code x} lê do {@code payload}. Aceita aninhamento por ponto
 * ({@code order.total}).
 */
public record EvaluationContext(String eventId,
                                String eventType,
                                String userId,
                                Map<String, Object> payload,
                                Map<String, Object> userAttributes) {

    public EvaluationContext {
        payload = payload == null ? Map.of() : payload;
        userAttributes = userAttributes == null ? Map.of() : userAttributes;
    }

    public Object resolve(String path) {
        if (path == null || path.isBlank()) {
            return null;
        }
        Map<String, Object> root;
        String rest;
        if (path.startsWith("user.")) {
            root = userAttributes;
            rest = path.substring("user.".length());
        } else if (path.startsWith("event.")) {
            root = payload;
            rest = path.substring("event.".length());
        } else {
            root = payload;
            rest = path;
        }
        Object current = root;
        for (String part : rest.split("\\.")) {
            if (current instanceof Map<?, ?> map) {
                current = map.get(part);
            } else {
                return null;
            }
        }
        return current;
    }
}
