package com.example.notifyengine.domain.engine.operator;

import com.example.notifyengine.domain.exception.InvalidRuleException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Catálogo de operadores: os nativos mais qualquer {@link OperatorStrategy} registrado como bean. */
@Component
public class OperatorRegistry {

    private final Map<String, OperatorStrategy> operators = new HashMap<>();

    @Autowired
    public OperatorRegistry(ObjectProvider<OperatorStrategy> custom) {
        this(custom.orderedStream().toList());
    }

    public OperatorRegistry(List<OperatorStrategy> custom) {
        BuiltInOperators.all().forEach(this::register);
        custom.forEach(this::register);
    }

    private void register(OperatorStrategy operator) {
        operators.put(operator.name().toUpperCase(Locale.ROOT), operator);
    }

    public OperatorStrategy get(String name) {
        OperatorStrategy operator = name == null ? null : operators.get(name.toUpperCase(Locale.ROOT));
        if (operator == null) {
            throw new InvalidRuleException("Operador desconhecido: " + name
                    + ". Disponíveis: " + operators.keySet());
        }
        return operator;
    }
}
