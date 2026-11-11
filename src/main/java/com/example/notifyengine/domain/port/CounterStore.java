package com.example.notifyengine.domain.port;

import java.time.Duration;

/** Armazenamento de contadores e marcadores com expiração (implementado em Redis). */
public interface CounterStore {

    long count(String key);

    /** Incrementa o contador; o TTL é aplicado quando o contador é criado (janela fixa). */
    void increment(String key, Duration ttlIfNew);

    boolean exists(String key);

    void mark(String key, Duration ttl);
}
