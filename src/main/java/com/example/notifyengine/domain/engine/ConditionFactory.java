package com.example.notifyengine.domain.engine;

import com.example.notifyengine.domain.engine.operator.OperatorRegistry;
import com.example.notifyengine.domain.exception.InvalidRuleException;
import com.example.notifyengine.domain.model.ConditionNode;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Locale;

/** Converte a árvore JSON ({@link ConditionNode}) em objetos {@link Condition}, validando a estrutura. */
@Component
public class ConditionFactory {

    /** Limites contra árvores gigantes ou profundas demais (CWE-400 / CWE-674). */
    static final int MAX_DEPTH = 10;
    static final int MAX_NODES = 200;
    private static final int MAX_IN_LIST = 1000;

    private final OperatorRegistry operators;

    public ConditionFactory(OperatorRegistry operators) {
        this.operators = operators;
    }

    public Condition build(ConditionNode node) {
        return build(node, 1, new int[]{0});
    }

    private Condition build(ConditionNode node, int depth, int[] nodeCount) {
        if (node == null) {
            throw new InvalidRuleException("A condição é obrigatória");
        }
        if (depth > MAX_DEPTH) {
            throw new InvalidRuleException("Condição aninhada demais (máximo " + MAX_DEPTH + " níveis)");
        }
        if (++nodeCount[0] > MAX_NODES) {
            throw new InvalidRuleException("Condição com nós demais (máximo " + MAX_NODES + ")");
        }
        String type = node.type() == null ? "" : node.type().toUpperCase(Locale.ROOT);
        return switch (type) {
            case "AND" -> new Condition.And(buildChildren(node, type, depth, nodeCount));
            case "OR" -> new Condition.Or(buildChildren(node, type, depth, nodeCount));
            case "LEAF" -> buildLeaf(node);
            default -> throw new InvalidRuleException(
                    "Tipo de condição inválido: '" + node.type() + "'. Use AND, OR ou LEAF");
        };
    }

    private List<Condition> buildChildren(ConditionNode node, String type, int depth, int[] nodeCount) {
        if (node.children() == null || node.children().isEmpty()) {
            throw new InvalidRuleException("Condição " + type + " exige ao menos um filho");
        }
        return node.children().stream().map(child -> build(child, depth + 1, nodeCount)).toList();
    }

    private Condition buildLeaf(ConditionNode node) {
        if (node.field() == null || node.field().isBlank()) {
            throw new InvalidRuleException("Condição LEAF exige o campo 'field'");
        }
        var operator = operators.get(node.op());
        if (node.value() == null) {
            throw new InvalidRuleException("Condição LEAF exige o campo 'value' (campo: " + node.field() + ")");
        }
        if ("IN".equalsIgnoreCase(node.op())) {
            if (!(node.value() instanceof Collection<?> list)) {
                throw new InvalidRuleException(
                        "O operador IN exige uma lista em 'value' (campo: " + node.field() + ")");
            }
            if (list.size() > MAX_IN_LIST) {
                throw new InvalidRuleException("Lista do operador IN grande demais (máximo " + MAX_IN_LIST + ")");
            }
        }
        return new Condition.Leaf(node.field(), operator, node.value());
    }
}
