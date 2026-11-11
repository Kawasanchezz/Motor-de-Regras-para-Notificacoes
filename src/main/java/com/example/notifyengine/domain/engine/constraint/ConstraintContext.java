package com.example.notifyengine.domain.engine.constraint;

import com.example.notifyengine.domain.model.ActionSpec;
import com.example.notifyengine.domain.model.EvaluationContext;
import com.example.notifyengine.domain.model.Rule;

import java.time.Instant;

/** Tudo o que uma restrição precisa para decidir: regra, ação (canal), evento e instante atual. */
public record ConstraintContext(Rule rule, ActionSpec action, EvaluationContext event, Instant now) {

    /** Escopo de contadores: regra + usuário + canal. */
    public String scope() {
        return rule.id() + ":" + event.userId() + ":" + action.channel();
    }
}
