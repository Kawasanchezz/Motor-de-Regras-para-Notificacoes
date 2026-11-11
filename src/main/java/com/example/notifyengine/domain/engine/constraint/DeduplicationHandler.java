package com.example.notifyengine.domain.engine.constraint;

import com.example.notifyengine.domain.model.ConstraintSpec;
import com.example.notifyengine.domain.model.ConstraintType;
import com.example.notifyengine.domain.model.Decision;
import com.example.notifyengine.domain.port.CounterStore;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * DEDUP: bloqueia notificação repetida dentro de {@code ttlSeconds} (padrão 3600).
 * A identidade é o hash dos {@code fields} informados (caminhos no contexto); sem {@code fields},
 * usa o payload inteiro do evento.
 */
@Component
public class DeduplicationHandler implements ConstraintHandler {

    private final CounterStore store;

    public DeduplicationHandler(CounterStore store) {
        this.store = store;
    }

    @Override
    public int order() {
        return 20;
    }

    @Override
    public ConstraintType type() {
        return ConstraintType.DEDUP;
    }

    @Override
    public void validate(Map<String, Object> params) {
        ParamReader.positiveLong(params, "ttlSeconds", 3600L);
        ParamReader.stringList(params, "fields");
    }

    @Override
    public Decision check(ConstraintContext ctx) {
        Optional<ConstraintSpec> spec = ctx.rule().constraint(ConstraintType.DEDUP);
        if (spec.isEmpty()) {
            return PASS;
        }
        return store.exists(key(ctx, spec.get())) ? Decision.block("DUPLICATE_NOTIFICATION") : PASS;
    }

    @Override
    public void commit(ConstraintContext ctx) {
        ctx.rule().constraint(ConstraintType.DEDUP).ifPresent(spec -> {
            long ttl = ParamReader.positiveLong(spec.params(), "ttlSeconds", 3600L);
            store.mark(key(ctx, spec), Duration.ofSeconds(ttl));
        });
    }

    private String key(ConstraintContext ctx, ConstraintSpec spec) {
        List<String> fields = ParamReader.stringList(spec.params(), "fields");
        String identity = fields.isEmpty()
                ? String.valueOf(ctx.event().payload())
                : fields.stream()
                        .map(f -> f + "=" + ctx.event().resolve(f))
                        .collect(Collectors.joining("|"));
        return "dedup:" + ctx.scope() + ":" + sha256(identity);
    }

    private static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
