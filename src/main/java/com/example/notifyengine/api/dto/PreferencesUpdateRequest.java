package com.example.notifyengine.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record PreferencesUpdateRequest(@NotEmpty @Size(max = 50) @Valid List<PreferenceRequest> preferences) {
}
