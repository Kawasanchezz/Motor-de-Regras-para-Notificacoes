package com.example.notifyengine.domain;

import com.example.notifyengine.domain.engine.ConditionFactory;
import com.example.notifyengine.domain.engine.operator.OperatorRegistry;
import com.example.notifyengine.domain.exception.InvalidRuleException;
import com.example.notifyengine.domain.model.ConditionNode;
import com.example.notifyengine.domain.model.EvaluationContext;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConditionFactoryTest {

    private final ConditionFactory factory = new ConditionFactory(new OperatorRegistry(List.of()));

    private final EvaluationContext context = new EvaluationContext("e1", "PEDIDO_CRIADO", "u1",
            Map.of("order", Map.of("total", 150, "id", "A1", "items", List.of("book", "pen")),
                    "status", "NEW"),
            Map.of("country", "BR", "name", "Ana"));

    private static ConditionNode leaf(String field, String op, Object value) {
        return new ConditionNode("LEAF", field, op, value, null);
    }

    private static ConditionNode group(String type, ConditionNode... children) {
        return new ConditionNode(type, null, null, null, List.of(children));
    }

    private boolean eval(ConditionNode node) {
        return factory.build(node).evaluate(context);
    }

    @Test
    void numericOperators() {
        assertThat(eval(leaf("order.total", "GT", 100))).isTrue();
        assertThat(eval(leaf("order.total", "GT", 150))).isFalse();
        assertThat(eval(leaf("order.total", "GTE", 150))).isTrue();
        assertThat(eval(leaf("order.total", "LT", 200))).isTrue();
        assertThat(eval(leaf("order.total", "LTE", 149))).isFalse();
        assertThat(eval(leaf("order.total", "EQ", 150.0))).isTrue();
    }

    @Test
    void stringAndListOperators() {
        assertThat(eval(leaf("status", "EQ", "NEW"))).isTrue();
        assertThat(eval(leaf("status", "NEQ", "NEW"))).isFalse();
        assertThat(eval(leaf("order.id", "CONTAINS", "A"))).isTrue();
        assertThat(eval(leaf("order.items", "CONTAINS", "pen"))).isTrue();
        assertThat(eval(leaf("order.items", "CONTAINS", "car"))).isFalse();
        assertThat(eval(leaf("status", "IN", List.of("NEW", "PAID")))).isTrue();
        assertThat(eval(leaf("status", "IN", List.of("PAID")))).isFalse();
    }

    @Test
    void userAttributesAreResolvedWithUserPrefix() {
        assertThat(eval(leaf("user.country", "EQ", "BR"))).isTrue();
        assertThat(eval(leaf("user.country", "EQ", "US"))).isFalse();
    }

    @Test
    void missingFieldNeverMatchesExceptNeq() {
        assertThat(eval(leaf("order.missing", "EQ", "x"))).isFalse();
        assertThat(eval(leaf("order.missing", "GT", 1))).isFalse();
        assertThat(eval(leaf("order.missing", "NEQ", "x"))).isTrue();
    }

    @Test
    void nestedAndOr() {
        ConditionNode tree = group("AND",
                leaf("order.total", "GT", 100),
                group("OR", leaf("status", "EQ", "PAID"), leaf("user.country", "EQ", "BR")));
        assertThat(eval(tree)).isTrue();

        ConditionNode failing = group("AND",
                leaf("order.total", "GT", 100),
                group("OR", leaf("status", "EQ", "PAID"), leaf("user.country", "EQ", "US")));
        assertThat(eval(failing)).isFalse();
    }

    @Test
    void invalidStructuresAreRejected() {
        assertThatThrownBy(() -> factory.build(null)).isInstanceOf(InvalidRuleException.class);
        assertThatThrownBy(() -> factory.build(leaf("a", "NOPE", 1))).isInstanceOf(InvalidRuleException.class);
        assertThatThrownBy(() -> factory.build(leaf("a", "IN", "not-a-list"))).isInstanceOf(InvalidRuleException.class);
        assertThatThrownBy(() -> factory.build(leaf("a", "EQ", null))).isInstanceOf(InvalidRuleException.class);
        assertThatThrownBy(() -> factory.build(group("AND"))).isInstanceOf(InvalidRuleException.class);
        assertThatThrownBy(() -> factory.build(new ConditionNode("XOR", null, null, null, null)))
                .isInstanceOf(InvalidRuleException.class);
    }

    @Test
    void tooDeeplyNestedConditionIsRejected() {
        ConditionNode node = leaf("a", "EQ", 1);
        for (int i = 0; i < 15; i++) {
            node = group("AND", node);
        }
        ConditionNode tooDeep = node;
        assertThatThrownBy(() -> factory.build(tooDeep))
                .isInstanceOf(InvalidRuleException.class)
                .hasMessageContaining("aninhada");
    }

    @Test
    void conditionWithTooManyNodesIsRejected() {
        ConditionNode[] leaves = new ConditionNode[250];
        java.util.Arrays.fill(leaves, leaf("a", "EQ", 1));
        assertThatThrownBy(() -> factory.build(group("OR", leaves)))
                .isInstanceOf(InvalidRuleException.class)
                .hasMessageContaining("nós demais");
    }

    @Test
    void customOperatorIsDiscovered() {
        var custom = com.example.notifyengine.domain.engine.operator.OperatorStrategy.of("STARTS_WITH",
                (actual, expected) -> actual != null && String.valueOf(actual).startsWith(String.valueOf(expected)));
        ConditionFactory extended = new ConditionFactory(new OperatorRegistry(List.of(custom)));
        assertThat(extended.build(leaf("order.id", "STARTS_WITH", "A")).evaluate(context)).isTrue();
    }
}
