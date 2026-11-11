package com.example.notifyengine.application;

import com.example.notifyengine.api.dto.EventResponse;
import com.example.notifyengine.application.exception.IdempotencyConflictException;
import com.example.notifyengine.domain.port.KeyValueStore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.function.Supplier;

/**
 * Garante que um mesmo evento (mesma chave) seja processado uma única vez.
 * Reenvios devolvem a resposta original com {@code replayed=true}.
 */
@Service
public class IdempotencyService {

    private static final String PREFIX = "idem:";
    private static final String PROCESSING = "PROCESSING";
    private static final Duration LOCK_TTL = Duration.ofSeconds(30);

    private final KeyValueStore store;
    private final ObjectMapper mapper;
    private final Duration resultTtl;

    public IdempotencyService(KeyValueStore store,
                              ObjectMapper mapper,
                              @Value("${notify.idempotency.ttl:24h}") Duration resultTtl) {
        this.store = store;
        this.mapper = mapper;
        this.resultTtl = resultTtl;
    }

    public EventResponse execute(String key, Supplier<EventResponse> action) {
        String storeKey = PREFIX + key;

        String existing = store.get(storeKey);
        if (existing != null) {
            return replay(key, existing);
        }

        if (!store.setIfAbsent(storeKey, PROCESSING, LOCK_TTL)) {
            // Outra requisição venceu a corrida.
            String current = store.get(storeKey);
            if (current != null) {
                return replay(key, current);
            }
            throw new IdempotencyConflictException(key);
        }

        try {
            EventResponse response = action.get();
            store.set(storeKey, toJson(response), resultTtl);
            return response;
        } catch (RuntimeException e) {
            store.delete(storeKey);
            throw e;
        }
    }

    private EventResponse replay(String key, String stored) {
        if (PROCESSING.equals(stored)) {
            throw new IdempotencyConflictException(key);
        }
        try {
            return mapper.readValue(stored, EventResponse.class).withReplayed(true);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Resposta de idempotência corrompida para a chave " + key, e);
        }
    }

    private String toJson(EventResponse response) {
        try {
            return mapper.writeValueAsString(response);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Falha ao serializar resposta", e);
        }
    }
}
