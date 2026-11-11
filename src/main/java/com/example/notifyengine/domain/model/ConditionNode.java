package com.example.notifyengine.domain.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * Representação serializável (JSON) da árvore de condições.
 * <ul>
 *   <li>type = AND | OR: usa {@code children}</li>
 *   <li>type = LEAF: usa {@code field}, {@code op} e {@code value}</li>
 * </ul>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ConditionNode(String type, String field, String op, Object value, List<ConditionNode> children) {
}
