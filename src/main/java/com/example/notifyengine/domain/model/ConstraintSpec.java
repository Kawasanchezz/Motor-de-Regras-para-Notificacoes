package com.example.notifyengine.domain.model;

import java.util.Map;

public record ConstraintSpec(ConstraintType type, Map<String, Object> params) {

    public ConstraintSpec {
        params = params == null ? Map.of() : params;
    }
}
