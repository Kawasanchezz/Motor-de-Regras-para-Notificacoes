package com.example.notifyengine.domain.exception;

/** Regra malformada (condição, operador ou parâmetro de restrição inválido). */
public class InvalidRuleException extends RuntimeException {

    public InvalidRuleException(String message) {
        super(message);
    }
}
