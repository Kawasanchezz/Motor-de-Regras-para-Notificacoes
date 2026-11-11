package com.example.notifyengine.api;

import com.example.notifyengine.api.dto.PreferenceResponse;
import com.example.notifyengine.api.dto.PreferencesUpdateRequest;
import com.example.notifyengine.application.PreferenceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Tag(name = "Preferências", description = "Opt-out por canal e tipo de evento")
public class PreferenceController {

    private final PreferenceService service;

    public PreferenceController(PreferenceService service) {
        this.service = service;
    }

    @PutMapping("/users/{userId}/preferences")
    @Operation(summary = "Cria ou atualiza preferências do usuário")
    public List<PreferenceResponse> update(@PathVariable @Size(max = 100) String userId,
                                           @Valid @RequestBody PreferencesUpdateRequest request) {
        return service.update(userId, request.preferences());
    }

    @GetMapping("/users/{userId}/preferences")
    @Operation(summary = "Lista as preferências do usuário")
    public List<PreferenceResponse> list(@PathVariable @Size(max = 100) String userId) {
        return service.list(userId);
    }
}
