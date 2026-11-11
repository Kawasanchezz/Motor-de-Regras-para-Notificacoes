package com.example.notifyengine.domain.engine.constraint;

import com.example.notifyengine.domain.model.ConstraintSpec;
import com.example.notifyengine.domain.model.Decision;
import com.example.notifyengine.domain.model.DecisionType;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

/** Executa os handlers em ordem; o primeiro que não devolver SEND encerra a cadeia. */
@Component
public class ConstraintChain {

    private final List<ConstraintHandler> handlers;

    public ConstraintChain(List<ConstraintHandler> handlers) {
        this.handlers = handlers.stream().sorted(Comparator.comparingInt(ConstraintHandler::order)).toList();
    }

    public Decision evaluate(ConstraintContext context) {
        for (ConstraintHandler handler : handlers) {
            Decision decision = handler.check(context);
            if (decision.type() != DecisionType.SEND) {
                return decision;
            }
        }
        return ConstraintHandler.PASS;
    }

    /** Registra o consumo das restrições após envio/agendamento bem-sucedido. */
    public void commit(ConstraintContext context) {
        handlers.forEach(handler -> handler.commit(context));
    }

    public void validate(ConstraintSpec spec) {
        handlers.stream()
                .filter(handler -> handler.type() == spec.type())
                .forEach(handler -> handler.validate(spec.params()));
    }
}
