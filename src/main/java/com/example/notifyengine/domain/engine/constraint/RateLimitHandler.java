package com.example.notifyengine.domain.engine.constraint;

import com.example.notifyengine.domain.model.ConstraintSpec;
import com.example.notifyengine.domain.model.ConstraintType;
import com.example.notifyengine.domain.model.Decision;
import com.example.notifyengine.domain.port.CounterStore;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

/** RATE_LIMIT: no máximo {@code max} envios por usuário/regra/canal em janela fixa de {@code windowSeconds}. */
@Component
public class RateLimitHandler implements ConstraintHandler {

    private final CounterStore store;

    public RateLimitHandler(CounterStore store) {
        this.store = store;
    }

    @Override
    public int order() {
        return 40;
    }

    @Override
    public ConstraintType type() {
        return ConstraintType.RATE_LIMIT;
    }

    @Override
    public void validate(Map<String, Object> params) {
        ParamReader.positiveLong(params, "max", null);
        ParamReader.positiveLong(params, "windowSeconds", null);
    }

    @Override
    public Decision check(ConstraintContext ctx) {
        Optional<ConstraintSpec> spec = ctx.rule().constraint(ConstraintType.RATE_LIMIT);
        if (spec.isEmpty()) {
            return PASS;
        }
        long max = ParamReader.positiveLong(spec.get().params(), "max", null);
        return store.count(key(ctx)) >= max ? Decision.block("RATE_LIMIT_EXCEEDED") : PASS;
    }

    @Override
    public void commit(ConstraintContext ctx) {
        ctx.rule().constraint(ConstraintType.RATE_LIMIT).ifPresent(spec -> {
            long window = ParamReader.positiveLong(spec.params(), "windowSeconds", null);
            store.increment(key(ctx), Duration.ofSeconds(window));
        });
    }

    private String key(ConstraintContext ctx) {
        return "rate:" + ctx.scope();
    }
}
