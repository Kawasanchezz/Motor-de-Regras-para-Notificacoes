package com.example.notifyengine.api.dto;

import com.example.notifyengine.domain.model.ConditionNode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record RuleRequest(@NotBlank @Size(max = 150) String name,
                          @Size(max = 2000) String description,
                          @NotBlank @Size(max = 100) String eventType,
                          @NotNull Integer priority,
                          Boolean active,
                          @NotNull ConditionNode condition,
                          @NotEmpty @Size(max = 10) @Valid List<ActionDto> actions,
                          @Size(max = 10) @Valid List<ConstraintDto> constraints) {
}
