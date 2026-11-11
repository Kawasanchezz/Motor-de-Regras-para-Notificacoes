package com.example.notifyengine.infra.memory;

import com.example.notifyengine.domain.port.CounterStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

/** Versão em memória (apenas desenvolvimento local, uma única instância): {@code notify.store=memory}. */
@Component
@ConditionalOnProperty(name = "notify.store", havingValue = "memory")
public class InMemoryCounterStore implements CounterStore {

    private record Entry(long value, long expiresAtMillis) {
    }

    private final ConcurrentHashMap<String, Entry> map = new ConcurrentHashMap<>();
    private final Clock clock;

    public InMemoryCounterStore(Clock clock) {
        this.clock = clock;
    }

    @Override
    public long count(String key) {
        Entry entry = live(key);
        return entry == null ? 0 : entry.value();
    }

    @Override
    public void increment(String key, Duration ttlIfNew) {
        long now = clock.millis();
        map.compute(key, (k, current) -> (current == null || current.expiresAtMillis() <= now)
                ? new Entry(1, now + ttlIfNew.toMillis())
                : new Entry(current.value() + 1, current.expiresAtMillis()));
    }

    @Override
    public boolean exists(String key) {
        return live(key) != null;
    }

    @Override
    public void mark(String key, Duration ttl) {
        map.put(key, new Entry(1, clock.millis() + ttl.toMillis()));
    }

    /** Remove entradas vencidas que nunca foram lidas de novo (evita crescimento indefinido). */
    @Scheduled(fixedDelay = 60_000)
    void purgeExpired() {
        long now = clock.millis();
        map.values().removeIf(entry -> entry.expiresAtMillis() <= now);
    }

    private Entry live(String key) {
        Entry entry = map.get(key);
        if (entry == null) {
            return null;
        }
        if (entry.expiresAtMillis() <= clock.millis()) {
            map.remove(key, entry);
            return null;
        }
        return entry;
    }
}
