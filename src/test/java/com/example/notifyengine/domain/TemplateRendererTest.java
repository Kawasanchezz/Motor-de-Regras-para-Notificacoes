package com.example.notifyengine.domain;

import com.example.notifyengine.domain.engine.TemplateRenderer;
import com.example.notifyengine.domain.model.EvaluationContext;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TemplateRendererTest {

    private final EvaluationContext context = new EvaluationContext("e1", "E", "u1",
            Map.of("order", Map.of("id", "A1", "total", 99.9)),
            Map.of("name", "Ana", "email", "ana@example.com"));

    @Test
    void rendersPlaceholdersFromPayloadAndUser() {
        String result = TemplateRenderer.render("Olá {{user.name}}, pedido {{ order.id }} de R$ {{order.total}}", context);
        assertThat(result).isEqualTo("Olá Ana, pedido A1 de R$ 99.9");
    }

    @Test
    void unknownPlaceholderBecomesEmpty() {
        assertThat(TemplateRenderer.render("x{{nope}}y", context)).isEqualTo("xy");
    }

    @Test
    void recipientResolvesPathOrFallsBackToLiteral() {
        assertThat(TemplateRenderer.resolveRecipient("user.email", context)).isEqualTo("ana@example.com");
        assertThat(TemplateRenderer.resolveRecipient("ops@example.com", context)).isEqualTo("ops@example.com");
    }
}
