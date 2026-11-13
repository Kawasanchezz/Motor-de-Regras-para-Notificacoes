package com.example.notifyengine.api;

import com.example.notifyengine.api.dto.EventRequest;
import com.example.notifyengine.api.dto.EventResponse;
import com.example.notifyengine.application.EventProcessingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Eventos", description = "Ingestão de eventos e decisão de notificação")
public class EventController {

    private final EventProcessingService service;

    public EventController(EventProcessingService service) {
        this.service = service;
    }

    @PostMapping("/events")
    @Operation(summary = "Avalia um evento e decide enviar, adiar ou bloquear",
            description = "Idempotente: usa o header Idempotency-Key, ou o eventId quando o header não é enviado.")
    public EventResponse ingest(@Valid @RequestBody EventRequest request,
                                @RequestHeader(value = "Idempotency-Key", required = false)
                                @Size(max = 150) String idempotencyKey) {
        return service.process(request, idempotencyKey);
    }
}
