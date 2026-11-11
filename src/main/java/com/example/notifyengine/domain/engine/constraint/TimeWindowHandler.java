package com.example.notifyengine.domain.engine.constraint;

import com.example.notifyengine.domain.exception.InvalidRuleException;
import com.example.notifyengine.domain.model.ConstraintSpec;
import com.example.notifyengine.domain.model.ConstraintType;
import com.example.notifyengine.domain.model.Decision;
import org.springframework.stereotype.Component;

import java.time.DateTimeException;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.Optional;

/**
 * TIME_WINDOW: só permite envio entre {@code start} e {@code end} (HH:mm) no fuso {@code zone}.
 * Fora da janela devolve DEFER com o próximo início. Suporta janelas que cruzam a meia-noite
 * (ex.: start 22:00, end 06:00).
 */
@Component
public class TimeWindowHandler implements ConstraintHandler {

    @Override
    public int order() {
        return 50;
    }

    @Override
    public ConstraintType type() {
        return ConstraintType.TIME_WINDOW;
    }

    @Override
    public void validate(Map<String, Object> params) {
        parse(params);
    }

    @Override
    public Decision check(ConstraintContext ctx) {
        Optional<ConstraintSpec> spec = ctx.rule().constraint(ConstraintType.TIME_WINDOW);
        if (spec.isEmpty()) {
            return PASS;
        }
        Window window = parse(spec.get().params());
        ZonedDateTime local = ctx.now().atZone(window.zone());
        LocalTime time = local.toLocalTime();

        if (window.contains(time)) {
            return PASS;
        }
        ZonedDateTime next = local.with(window.start());
        if (!next.isAfter(local)) {
            next = next.plusDays(1);
        }
        return Decision.defer("OUTSIDE_TIME_WINDOW", next.toInstant());
    }

    private static Window parse(Map<String, Object> params) {
        try {
            LocalTime start = LocalTime.parse(ParamReader.string(params, "start", null));
            LocalTime end = LocalTime.parse(ParamReader.string(params, "end", null));
            ZoneId zone = ZoneId.of(ParamReader.string(params, "zone", "UTC"));
            return new Window(start, end, zone);
        } catch (DateTimeParseException e) {
            throw new InvalidRuleException("Horário inválido em TIME_WINDOW (use HH:mm): " + e.getParsedString());
        } catch (DateTimeException e) {
            throw new InvalidRuleException("Fuso horário inválido em TIME_WINDOW: " + e.getMessage());
        }
    }

    private record Window(LocalTime start, LocalTime end, ZoneId zone) {

        boolean contains(LocalTime time) {
            if (start.equals(end)) {
                return true;
            }
            if (start.isBefore(end)) {
                return !time.isBefore(start) && time.isBefore(end);
            }
            return !time.isBefore(start) || time.isBefore(end);
        }
    }
}
