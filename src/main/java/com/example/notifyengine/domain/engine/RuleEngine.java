package com.example.notifyengine.domain.engine;

import com.example.notifyengine.domain.engine.constraint.ConstraintChain;
import com.example.notifyengine.domain.engine.constraint.ConstraintContext;
import com.example.notifyengine.domain.model.Decision;
import com.example.notifyengine.domain.model.EvaluationContext;
import com.example.notifyengine.domain.model.Rule;
import org.springframework.stereotype.Component;

/** Fachada do motor: avalia condições de uma regra e aplica a cadeia de restrições. */
@Component
public class RuleEngine {

    private final ConditionFactory conditionFactory;
    private final ConstraintChain constraintChain;

    public RuleEngine(ConditionFactory conditionFactory, ConstraintChain constraintChain) {
        this.conditionFactory = conditionFactory;
        this.constraintChain = constraintChain;
    }

    public boolean matches(Rule rule, EvaluationContext context) {
        return conditionFactory.build(rule.condition()).evaluate(context);
    }

    public Decision checkConstraints(ConstraintContext context) {
        return constraintChain.evaluate(context);
    }

    public void commit(ConstraintContext context) {
        constraintChain.commit(context);
    }
}
