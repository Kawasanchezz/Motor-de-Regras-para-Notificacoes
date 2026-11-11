package com.example.notifyengine.api;

import com.example.notifyengine.api.dto.DecisionAuditResponse;
import com.example.notifyengine.application.AuditService;
import com.example.notifyengine.domain.model.DecisionType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/decisions")
@Tag(name = "Histórico", description = "Auditoria das decisões do motor")
public class DecisionController {

    private final AuditService service;

    public DecisionController(AuditService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Consulta paginada do histórico de decisões, com filtros")
    public PagedModel<DecisionAuditResponse> search(
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) DecisionType decision,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return new PagedModel<>(service.search(userId, eventType, decision, from, to, pageable));
    }
}
