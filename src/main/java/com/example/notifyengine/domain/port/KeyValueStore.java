package com.example.notifyengine.domain.port;

import java.time.Duration;

/** Armazenamento chave/valor com expiração (cache de regras e idempotência). Redis ou memória. */
public interface KeyValueStore {

    /** @return o valor, ou null se ausente/expirado */
    String get(String key);

    void set(String key, String value, Duration ttl);

    /** @return true se gravou (a chave não existia) */
    boolean setIfAbsent(String key, String value, Duration ttl);

    void delete(String key);
}
