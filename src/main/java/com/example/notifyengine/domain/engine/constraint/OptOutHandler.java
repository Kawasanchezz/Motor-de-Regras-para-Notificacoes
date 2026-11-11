package com.example.notifyengine.domain.engine.constraint;

import com.example.notifyengine.domain.model.ConstraintType;
import com.example.notifyengine.domain.model.Decision;
import com.example.notifyengine.domain.port.OptOutPort;
import org.springframework.stereotype.Component;

/** Restrição global: respeita o opt-out do usuário por canal e tipo de evento. */
@Component
public class OptOutHandler implements ConstraintHandler {

    private final OptOutPort optOut;

    public OptOutHandler(OptOutPort optOut) {
        this.optOut = optOut;
    }

    @Override
    public int order() {
        return 10;
    }

    @Override
    public ConstraintType type() {
        return null;
    }

    @Override
    public Decision check(ConstraintContext ctx) {
        boolean optedOut = optOut.isOptedOut(ctx.event().userId(), ctx.action().channel(), ctx.event().eventType());
        return optedOut ? Decision.block("USER_OPTED_OUT") : PASS;
    }
}
