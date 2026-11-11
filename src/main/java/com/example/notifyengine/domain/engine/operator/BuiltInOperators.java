package com.example.notifyengine.domain.engine.operator;

import java.util.Collection;
import java.util.List;
import java.util.function.IntPredicate;

/** Operadores nativos: EQ, NEQ, GT, GTE, LT, LTE, CONTAINS, IN. */
final class BuiltInOperators {

    private BuiltInOperators() {
    }

    static List<OperatorStrategy> all() {
        return List.of(
                OperatorStrategy.of("EQ", Values::areEqual),
                OperatorStrategy.of("NEQ", (actual, expected) -> !Values.areEqual(actual, expected)),
                OperatorStrategy.of("GT", (actual, expected) -> compareWith(actual, expected, c -> c > 0)),
                OperatorStrategy.of("GTE", (actual, expected) -> compareWith(actual, expected, c -> c >= 0)),
                OperatorStrategy.of("LT", (actual, expected) -> compareWith(actual, expected, c -> c < 0)),
                OperatorStrategy.of("LTE", (actual, expected) -> compareWith(actual, expected, c -> c <= 0)),
                OperatorStrategy.of("CONTAINS", BuiltInOperators::contains),
                OperatorStrategy.of("IN", BuiltInOperators::in)
        );
    }

    private static boolean compareWith(Object actual, Object expected, IntPredicate predicate) {
        Integer result = Values.compare(actual, expected);
        return result != null && predicate.test(result);
    }

    private static boolean contains(Object actual, Object expected) {
        if (actual instanceof Collection<?> collection) {
            return collection.stream().anyMatch(item -> Values.areEqual(item, expected));
        }
        return actual != null && expected != null && String.valueOf(actual).contains(String.valueOf(expected));
    }

    private static boolean in(Object actual, Object expected) {
        return expected instanceof Collection<?> collection
                && collection.stream().anyMatch(item -> Values.areEqual(actual, item));
    }
}
