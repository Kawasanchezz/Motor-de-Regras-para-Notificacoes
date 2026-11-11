package com.example.notifyengine.domain.engine;

import com.example.notifyengine.domain.engine.operator.OperatorStrategy;
import com.example.notifyengine.domain.model.EvaluationContext;

import java.util.List;

/** Composite: nós AND/OR (compostos) e folhas de comparação. */
public interface Condition {

    boolean evaluate(EvaluationContext context);

    record And(List<Condition> children) implements Condition {
        @Override
        public boolean evaluate(EvaluationContext context) {
            return children.stream().allMatch(c -> c.evaluate(context));
        }
    }

    record Or(List<Condition> children) implements Condition {
        @Override
        public boolean evaluate(EvaluationContext context) {
            return children.stream().anyMatch(c -> c.evaluate(context));
        }
    }

    record Leaf(String field, OperatorStrategy operator, Object expected) implements Condition {
        @Override
        public boolean evaluate(EvaluationContext context) {
            return operator.test(context.resolve(field), expected);
        }
    }
}
