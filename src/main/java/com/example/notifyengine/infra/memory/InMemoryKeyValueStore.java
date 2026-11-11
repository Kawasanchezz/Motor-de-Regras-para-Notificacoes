package com.example.notifyengine.infra.memory;

import com.example.notifyengine.domain.port.KeyValueStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

/** Versão em memória (apenas desenvolvimento local, uma única instância): {@code notify.store=memory}. */
@Component
@ConditionalOnProperty(name = "notify.store", havingValue = "memory")
public class InMemoryKeyValueStore implements KeyValueStore {

    private record Entry(String value, long expiresAtMillis) {
    }

    private final ConcurrentHashMap<String, Entry> map = new ConcurrentHashMap<>();
    private final Clock clock;

    public InMemoryKeyValueStore(Clock clock) {
        this.clock = clock;
    }

    @Override
    public String get(String key) {
        Entry entry = map.get(key);
        if (entry == null) {
            return null;
        }
        if (entry.expiresAtMillis() <= clock.millis()) {
            map.remove(key, entry);
            return null;
        }
        return entry.value();
    }

    @Override
    public void set(String key, String value, Duration ttl) {
        map.put(key, new Entry(value, clock.millis() + ttl.toMillis()));
    }

    @Override
    public boolean setIfAbsent(String key, String value, Duration ttl) {
        boolean[] written = {false};
        long now = clock.millis();
        map.compute(key, (k, current) -> {
            if (current == null || current.expiresAtMillis() <= now) {
                written[0] = true;
                return new Entry(value, now + ttl.toMillis());
            }
            return current;
        });
        return written[0];
    }

    @Override
    public void delete(String key) {
        map.remove(key);
    }

    /** Remove entradas vencidas que nunca foram lidas de novo (evita crescimento indefinido). */
    @Scheduled(fixedDelay = 60_000)
    void purgeExpired() {
        long now = clock.millis();
        map.values().removeIf(entry -> entry.expiresAtMillis() <= now);
    }
}
