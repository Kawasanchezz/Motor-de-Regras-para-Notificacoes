package com.example.notifyengine.domain;

import com.example.notifyengine.domain.engine.constraint.ConstraintChain;
import com.example.notifyengine.domain.engine.constraint.ConstraintContext;
import com.example.notifyengine.domain.engine.constraint.CooldownHandler;
import com.example.notifyengine.domain.engine.constraint.DeduplicationHandler;
import com.example.notifyengine.domain.engine.constraint.OptOutHandler;
import com.example.notifyengine.domain.engine.constraint.RateLimitHandler;
import com.example.notifyengine.domain.engine.constraint.TimeWindowHandler;
import com.example.notifyengine.domain.exception.InvalidRuleException;
import com.example.notifyengine.domain.model.ActionSpec;
import com.example.notifyengine.domain.model.Channel;
import com.example.notifyengine.domain.model.ConstraintSpec;
import com.example.notifyengine.domain.model.ConstraintType;
import com.example.notifyengine.domain.model.Decision;
import com.example.notifyengine.domain.model.DecisionType;
import com.example.notifyengine.domain.model.EvaluationContext;
import com.example.notifyengine.domain.model.Rule;
import com.example.notifyengine.infra.memory.InMemoryCounterStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConstraintChainTest {

    private static final Instant NOW = Instant.parse("2025-01-15T15:00:00Z");
    private static final ActionSpec EMAIL = new ActionSpec(Channel.EMAIL, "hi", "user.email");

    private InMemoryCounterStore store;
    private AtomicBoolean optedOut;
    private ConstraintChain chain;

    @BeforeEach
    void setUp() {
        store = new InMemoryCounterStore(Clock.systemUTC());
        optedOut = new AtomicBoolean(false);
        chain = new ConstraintChain(List.of(
                new TimeWindowHandler(),
                new RateLimitHandler(store),
                new CooldownHandler(store),
                new DeduplicationHandler(store),
                new OptOutHandler((user, channel, eventType) -> optedOut.get())));
    }

    private static Rule rule(ConstraintSpec... constraints) {
        return new Rule(UUID.randomUUID(), "r", "E", 1, null, List.of(EMAIL), List.of(constraints));
    }

    private static ConstraintContext ctx(Rule rule, Instant now, Map<String, Object> payload) {
        return new ConstraintContext(rule, EMAIL, new EvaluationContext("e1", "E", "u1", payload, Map.of()), now);
    }

    private static ConstraintSpec spec(ConstraintType type, Map<String, Object> params) {
        return new ConstraintSpec(type, params);
    }

    @Test
    void ruleWithoutConstraintsPasses() {
        assertThat(chain.evaluate(ctx(rule(), NOW, Map.of())).type()).isEqualTo(DecisionType.SEND);
    }

    @Test
    void optOutBlocksBeforeAnyOtherConstraint() {
        optedOut.set(true);
        Rule rule = rule(spec(ConstraintType.TIME_WINDOW, Map.of("start", "00:00", "end", "01:00", "zone", "UTC")));

        Decision decision = chain.evaluate(ctx(rule, NOW, Map.of()));

        assertThat(decision.type()).isEqualTo(DecisionType.BLOCK);
        assertThat(decision.reason()).isEqualTo("USER_OPTED_OUT");
    }

    @Test
    void cooldownBlocksAfterCommit() {
        Rule rule = rule(spec(ConstraintType.COOLDOWN, Map.of("seconds", 60)));
        ConstraintContext context = ctx(rule, NOW, Map.of());

        assertThat(chain.evaluate(context).type()).isEqualTo(DecisionType.SEND);
        chain.commit(context);

        Decision second = chain.evaluate(context);
        assertThat(second.type()).isEqualTo(DecisionType.BLOCK);
        assertThat(second.reason()).isEqualTo("COOLDOWN_ACTIVE");
    }

    @Test
    void checkAloneDoesNotConsumeQuota() {
        Rule rule = rule(spec(ConstraintType.COOLDOWN, Map.of("seconds", 60)));
        ConstraintContext context = ctx(rule, NOW, Map.of());

        chain.evaluate(context);
        chain.evaluate(context);

        assertThat(chain.evaluate(context).type()).isEqualTo(DecisionType.SEND);
    }

    @Test
    void rateLimitBlocksAfterMaxSends() {
        Rule rule = rule(spec(ConstraintType.RATE_LIMIT, Map.of("max", 2, "windowSeconds", 3600)));
        ConstraintContext context = ctx(rule, NOW, Map.of());

        for (int i = 0; i < 2; i++) {
            assertThat(chain.evaluate(context).type()).isEqualTo(DecisionType.SEND);
            chain.commit(context);
        }

        Decision third = chain.evaluate(context);
        assertThat(third.type()).isEqualTo(DecisionType.BLOCK);
        assertThat(third.reason()).isEqualTo("RATE_LIMIT_EXCEEDED");
    }

    @Test
    void dedupBlocksSamePayloadButNotDifferentPayload() {
        Rule rule = rule(spec(ConstraintType.DEDUP, Map.of("ttlSeconds", 600)));
        ConstraintContext first = ctx(rule, NOW, Map.of("order", "A1"));
        chain.commit(first);

        assertThat(chain.evaluate(first).reason()).isEqualTo("DUPLICATE_NOTIFICATION");
        assertThat(chain.evaluate(ctx(rule, NOW, Map.of("order", "B2"))).type()).isEqualTo(DecisionType.SEND);
    }

    @Test
    void dedupCanUseSelectedFields() {
        Rule rule = rule(spec(ConstraintType.DEDUP, Map.of("fields", List.of("order.id"))));
        chain.commit(ctx(rule, NOW, Map.of("order", Map.of("id", "A1"), "noise", 1)));

        Decision sameOrder = chain.evaluate(ctx(rule, NOW, Map.of("order", Map.of("id", "A1"), "noise", 2)));

        assertThat(sameOrder.type()).isEqualTo(DecisionType.BLOCK);
    }

    @Test
    void timeWindowDefersToNextStartWhenOutside() {
        // 2025-01-15T01:00Z = 22:00 em São Paulo (UTC-3), fora da janela 08:00-22:00
        Rule rule = rule(spec(ConstraintType.TIME_WINDOW,
                Map.of("start", "08:00", "end", "22:00", "zone", "America/Sao_Paulo")));

        Decision decision = chain.evaluate(ctx(rule, Instant.parse("2025-01-15T01:00:00Z"), Map.of()));

        assertThat(decision.type()).isEqualTo(DecisionType.DEFER);
        assertThat(decision.reason()).isEqualTo("OUTSIDE_TIME_WINDOW");
        assertThat(decision.scheduledFor()).isEqualTo(Instant.parse("2025-01-15T11:00:00Z"));
    }

    @Test
    void timeWindowAllowsInsideWindow() {
        Rule rule = rule(spec(ConstraintType.TIME_WINDOW,
                Map.of("start", "08:00", "end", "22:00", "zone", "America/Sao_Paulo")));

        // 15:00Z = 12:00 em São Paulo
        assertThat(chain.evaluate(ctx(rule, NOW, Map.of())).type()).isEqualTo(DecisionType.SEND);
    }

    @Test
    void timeWindowDefersToTomorrowWhenPastTodaysStart() {
        Rule rule = rule(spec(ConstraintType.TIME_WINDOW, Map.of("start", "08:00", "end", "10:00", "zone", "UTC")));

        Decision decision = chain.evaluate(ctx(rule, Instant.parse("2025-01-15T12:00:00Z"), Map.of()));

        assertThat(decision.scheduledFor()).isEqualTo(Instant.parse("2025-01-16T08:00:00Z"));
    }

    @Test
    void timeWindowCrossingMidnight() {
        Rule rule = rule(spec(ConstraintType.TIME_WINDOW, Map.of("start", "22:00", "end", "06:00", "zone", "UTC")));

        assertThat(chain.evaluate(ctx(rule, Instant.parse("2025-01-15T23:00:00Z"), Map.of())).type())
                .isEqualTo(DecisionType.SEND);
        assertThat(chain.evaluate(ctx(rule, Instant.parse("2025-01-15T03:00:00Z"), Map.of())).type())
                .isEqualTo(DecisionType.SEND);

        Decision outside = chain.evaluate(ctx(rule, Instant.parse("2025-01-15T12:00:00Z"), Map.of()));
        assertThat(outside.type()).isEqualTo(DecisionType.DEFER);
        assertThat(outside.scheduledFor()).isEqualTo(Instant.parse("2025-01-15T22:00:00Z"));
    }

    @Test
    void absurdParameterValuesAreRejected() {
        assertThatThrownBy(() -> chain.validate(spec(ConstraintType.COOLDOWN, Map.of("seconds", Long.MAX_VALUE))))
                .isInstanceOf(InvalidRuleException.class);
        assertThatThrownBy(() -> chain.validate(
                spec(ConstraintType.RATE_LIMIT, Map.of("max", 5, "windowSeconds", 999_999_999_999L))))
                .isInstanceOf(InvalidRuleException.class);
    }

    @Test
    void invalidParametersAreRejectedOnValidation() {
        assertThatThrownBy(() -> chain.validate(spec(ConstraintType.COOLDOWN, Map.of())))
                .isInstanceOf(InvalidRuleException.class);
        assertThatThrownBy(() -> chain.validate(spec(ConstraintType.RATE_LIMIT, Map.of("max", 0, "windowSeconds", 10))))
                .isInstanceOf(InvalidRuleException.class);
        assertThatThrownBy(() -> chain.validate(spec(ConstraintType.TIME_WINDOW,
                Map.of("start", "8h", "end", "22:00"))))
                .isInstanceOf(InvalidRuleException.class);
        assertThatThrownBy(() -> chain.validate(spec(ConstraintType.TIME_WINDOW,
                Map.of("start", "08:00", "end", "22:00", "zone", "Mars/Olympus"))))
                .isInstanceOf(InvalidRuleException.class);
    }
}
