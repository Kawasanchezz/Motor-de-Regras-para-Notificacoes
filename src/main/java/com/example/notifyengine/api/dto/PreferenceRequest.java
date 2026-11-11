package com.example.notifyengine.api.dto;

import com.example.notifyengine.domain.model.Channel;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** eventType nulo significa "todos os tipos de evento". */
public record PreferenceRequest(@NotNull Channel channel,
                                @Size(max = 100) String eventType,
                                @NotNull Boolean optedOut) {
}
