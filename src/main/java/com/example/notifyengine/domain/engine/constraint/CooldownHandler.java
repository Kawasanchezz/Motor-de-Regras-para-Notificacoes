package com.example.notifyengine.domain.engine.constraint;

import com.example.notifyengine.domain.model.ConstraintType;
import com.example.notifyengine.domain.model.Decision;
import com.example.notifyengine.domain.port.CounterStore;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;

/** COOLDOWN: após um envio, bloqueia novos envios da mesma regra/usuário/canal por {@code seconds}. */
@Component
public class CooldownHandler implements ConstraintHandler {

    private final CounterStore store;

    public CooldownHandler(CounterStore store) {
        this.store = store;
    }

    @Override
    public int order() {
        return 30;
    }

    @Override
    public ConstraintType type() {
        return ConstraintType.COOLDOWN;
    }

    @Override
    public void validate(Map<String, Object> params) {
        ParamReader.positiveLong(params, "seconds", null);
    }

    @Override
    public Decision check(ConstraintContext ctx) {
        if (ctx.rule().constraint(ConstraintType.COOLDOWN).isEmpty()) {
            return PASS;
        }
        return store.exists(key(ctx)) ? Decision.block("COOLDOWN_ACTIVE") : PASS;
    }

    @Override
    public void commit(ConstraintContext ctx) {
        ctx.rule().constraint(ConstraintType.COOLDOWN).ifPresent(spec -> {
            long seconds = ParamReader.positiveLong(spec.params(), "seconds", null);
            store.mark(key(ctx), Duration.ofSeconds(seconds));
        });
    }

    private String key(ConstraintContext ctx) {
        return "cooldown:" + ctx.scope();
    }
}
