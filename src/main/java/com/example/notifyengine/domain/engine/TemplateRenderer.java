package com.example.notifyengine.domain.engine;

import com.example.notifyengine.domain.model.EvaluationContext;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Renderiza templates com placeholders {@code {{caminho}}} e resolve destinatários. */
public final class TemplateRenderer {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{\\s*([\\w.\\-]+)\\s*}}");

    private TemplateRenderer() {
    }

    public static String render(String template, EvaluationContext context) {
        if (template == null) {
            return "";
        }
        Matcher matcher = PLACEHOLDER.matcher(template);
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            Object value = context.resolve(matcher.group(1));
            matcher.appendReplacement(result, Matcher.quoteReplacement(value == null ? "" : String.valueOf(value)));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    /** Se a expressão resolver para um valor no contexto, usa-o; caso contrário trata como literal. */
    public static String resolveRecipient(String expression, EvaluationContext context) {
        Object value = context.resolve(expression);
        return value != null ? String.valueOf(value) : expression;
    }
}
