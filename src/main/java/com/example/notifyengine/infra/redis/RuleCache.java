package com.example.notifyengine.infra.redis;

import com.example.notifyengine.application.RuleQueryService;
import com.example.notifyengine.domain.model.Rule;
import com.example.notifyengine.domain.port.KeyValueStore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

/**
 * Cache das regras ativas por tipo de evento (cache-aside, invalidado a cada alteração de regra).
 * Usa o {@link KeyValueStore} ativo (Redis por padrão; memória no perfil local).
 */
@Component
public class RuleCache {

    private static final String PREFIX = "rules:active:";
    private static final Logger log = LoggerFactory.getLogger(RuleCache.class);

    private final KeyValueStore store;
    private final ObjectMapper mapper;
    private final RuleQueryService loader;
    private final Duration ttl;

    public RuleCache(KeyValueStore store,
                     ObjectMapper mapper,
                     RuleQueryService loader,
                     @Value("${notify.rules-cache.ttl:10m}") Duration ttl) {
        this.store = store;
        this.mapper = mapper;
        this.loader = loader;
        this.ttl = ttl;
    }

    public List<Rule> getActiveRules(String eventType) {
        String cached = store.get(PREFIX + eventType);
        if (cached != null) {
            try {
                return mapper.readValue(cached, new TypeReference<List<Rule>>() {
                });
            } catch (JsonProcessingException e) {
                log.warn("Cache de regras corrompido para {}; recarregando do banco", eventType, e);
            }
        }
        List<Rule> rules = loader.loadActive(eventType);
        try {
            store.set(PREFIX + eventType, mapper.writeValueAsString(rules), ttl);
        } catch (JsonProcessingException e) {
            log.warn("Não foi possível cachear regras de {}", eventType, e);
        }
        return rules;
    }

    public void invalidate(String eventType) {
        store.delete(PREFIX + eventType);
    }
}
