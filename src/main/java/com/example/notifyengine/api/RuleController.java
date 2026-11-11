package com.example.notifyengine.api;

import com.example.notifyengine.api.dto.RuleRequest;
import com.example.notifyengine.api.dto.RuleResponse;
import com.example.notifyengine.api.dto.StatusRequest;
import com.example.notifyengine.application.RuleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/rules")
@Tag(name = "Regras", description = "CRUD de regras de notificação")
public class RuleController {

    private final RuleService service;

    public RuleController(RuleService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Cria uma regra")
    public ResponseEntity<RuleResponse> create(@Valid @RequestBody RuleRequest request) {
        RuleResponse created = service.create(request);
        return ResponseEntity.created(URI.create("/rules/" + created.id())).body(created);
    }

    @GetMapping
    @Operation(summary = "Lista regras (paginado, filtro opcional por tipo de evento)")
    public PagedModel<RuleResponse> list(@RequestParam(required = false) String eventType,
                                         @PageableDefault(size = 20, sort = "priority") Pageable pageable) {
        return new PagedModel<>(service.list(eventType, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca uma regra pelo id")
    public RuleResponse get(@PathVariable UUID id) {
        return service.get(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza uma regra")
    public RuleResponse update(@PathVariable UUID id, @Valid @RequestBody RuleRequest request) {
        return service.update(id, request);
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Ativa ou desativa uma regra")
    public RuleResponse setStatus(@PathVariable UUID id, @Valid @RequestBody StatusRequest request) {
        return service.setActive(id, request.active());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Exclui uma regra")
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }
}
