package com.example.notifyengine.api.dto;

import com.example.notifyengine.domain.model.ConstraintType;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record ConstraintDto(@NotNull ConstraintType type, Map<String, Object> params) {
}
