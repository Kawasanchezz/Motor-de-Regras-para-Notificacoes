package com.example.notifyengine.domain.engine.constraint;

import com.example.notifyengine.domain.exception.InvalidRuleException;

import java.util.List;
import java.util.Map;

/** Leitura tipada dos parâmetros (JSON) das restrições. */
final class ParamReader {

    /** Limite superior de qualquer parâmetro numérico: 366 dias em segundos. */
    static final long MAX_VALUE = 366L * 24 * 3600;

    private ParamReader() {
    }

    /** Lê um inteiro positivo; se ausente usa o padrão, ou falha quando não há padrão. */
    static long positiveLong(Map<String, Object> params, String key, Long defaultValue) {
        Object raw = params.get(key);
        if (raw == null) {
            if (defaultValue == null) {
                throw new InvalidRuleException("Parâmetro obrigatório ausente: '" + key + "'");
            }
            return defaultValue;
        }
        long value;
        try {
            value = raw instanceof Number n ? n.longValue() : Long.parseLong(raw.toString().trim());
        } catch (NumberFormatException e) {
            throw new InvalidRuleException("Parâmetro '" + key + "' deve ser numérico");
        }
        if (value <= 0) {
            throw new InvalidRuleException("Parâmetro '" + key + "' deve ser maior que zero");
        }
        if (value > MAX_VALUE) {
            // Evita estouro de Duration/epoch e TTLs absurdos (CWE-190)
            throw new InvalidRuleException("Parâmetro '" + key + "' deve ser no máximo " + MAX_VALUE);
        }
        return value;
    }

    static String string(Map<String, Object> params, String key, String defaultValue) {
        Object raw = params.get(key);
        if (raw == null || raw.toString().isBlank()) {
            if (defaultValue == null) {
                throw new InvalidRuleException("Parâmetro obrigatório ausente: '" + key + "'");
            }
            return defaultValue;
        }
        return raw.toString().trim();
    }

    static List<String> stringList(Map<String, Object> params, String key) {
        Object raw = params.get(key);
        if (raw == null) {
            return List.of();
        }
        if (raw instanceof List<?> list) {
            return list.stream().map(String::valueOf).toList();
        }
        throw new InvalidRuleException("Parâmetro '" + key + "' deve ser uma lista");
    }
}
