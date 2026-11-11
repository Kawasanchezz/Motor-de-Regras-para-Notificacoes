package com.example.notifyengine.application.exception;

/** Já existe um processamento em andamento para a mesma chave de idempotência. */
public class IdempotencyConflictException extends RuntimeException {

    public IdempotencyConflictException(String key) {
        super("Evento com chave de idempotência '" + key + "' já está em processamento");
    }
}
