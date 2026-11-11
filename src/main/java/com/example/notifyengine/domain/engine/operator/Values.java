package com.example.notifyengine.domain.engine.operator;

import java.math.BigDecimal;

/** Utilitários de comparação tolerantes a tipos (Integer vs Double vs String numérica). */
final class Values {

    private Values() {
    }

    static boolean areEqual(Object a, Object b) {
        if (a == null || b == null) {
            return a == b;
        }
        BigDecimal[] numbers = numericPair(a, b);
        if (numbers != null) {
            return numbers[0].compareTo(numbers[1]) == 0;
        }
        return String.valueOf(a).equals(String.valueOf(b));
    }

    /** Retorna o resultado de compareTo, ou null se os valores não forem comparáveis. */
    static Integer compare(Object a, Object b) {
        if (a == null || b == null) {
            return null;
        }
        BigDecimal[] numbers = numericPair(a, b);
        if (numbers != null) {
            return numbers[0].compareTo(numbers[1]);
        }
        if (a instanceof String sa && b instanceof String sb) {
            return sa.compareTo(sb);
        }
        return null;
    }

    private static BigDecimal[] numericPair(Object a, Object b) {
        if (!(a instanceof Number) && !(b instanceof Number)) {
            return null;
        }
        BigDecimal x = parse(a);
        BigDecimal y = parse(b);
        return (x == null || y == null) ? null : new BigDecimal[]{x, y};
    }

    private static BigDecimal parse(Object o) {
        try {
            return new BigDecimal(o.toString().trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
