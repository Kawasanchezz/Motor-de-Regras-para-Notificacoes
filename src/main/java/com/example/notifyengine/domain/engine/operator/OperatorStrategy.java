package com.example.notifyengine.domain.engine.operator;

import java.util.function.BiPredicate;

/**
 * Strategy de um operador de comparação. Para criar um operador novo, registre um bean
 * que implemente esta interface; o {@link OperatorRegistry} o descobre automaticamente.
 */
public interface OperatorStrategy {

    /** Nome do operador usado no JSON da regra (ex.: EQ, GT). */
    String name();

    boolean test(Object actual, Object expected);

    static OperatorStrategy of(String name, BiPredicate<Object, Object> predicate) {
        return new OperatorStrategy() {
            @Override
            public String name() {
                return name;
            }

            @Override
            public boolean test(Object actual, Object expected) {
                return predicate.test(actual, expected);
            }
        };
    }
}
