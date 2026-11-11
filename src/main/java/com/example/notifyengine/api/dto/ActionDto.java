package com.example.notifyengine.api.dto;

import com.example.notifyengine.domain.model.Channel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ActionDto(@NotNull Channel channel,
                        @NotBlank @Size(max = 2000) String template,
                        @NotBlank @Size(max = 255) String recipient) {
}
