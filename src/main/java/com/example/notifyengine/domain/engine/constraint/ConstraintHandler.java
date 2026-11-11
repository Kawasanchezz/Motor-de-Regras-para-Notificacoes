package com.example.notifyengine.domain.engine.constraint;

import com.example.notifyengine.domain.model.ConstraintType;
import com.example.notifyengine.domain.model.Decision;

import java.util.Map;

/**
 * Elo da cadeia de restrições (Chain of Responsibility). Funciona em duas fases:
 * <ol>
 *   <li>{@link #check}: somente leitura; devolve SEND (passa), DEFER ou BLOCK;</li>
 *   <li>{@link #commit}: registra o consumo (contadores, cooldown) depois que a notificação
 *       foi efetivamente enviada ou agendada.</li>
 * </ol>
 * Para criar uma restrição nova, implemente esta interface como bean.
 */
public interface ConstraintHandler {

    /** Decisão devolvida quando a restrição não impede o envio. */
    Decision PASS = Decision.send("RULE_MATCHED");

    /** Ordem de execução (menor primeiro). */
    int order();

    /** Tipo configurável por regra que este handler trata; null para restrições globais. */
    ConstraintType type();

    Decision check(ConstraintContext context);

    default void commit(ConstraintContext context) {
    }

    /** Valida os parâmetros no momento da criação/atualização da regra. */
    default void validate(Map<String, Object> params) {
    }
}
