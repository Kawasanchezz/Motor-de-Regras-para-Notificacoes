package com.example.notifyengine.api.dto;

import java.time.Instant;
import java.util.List;

/** Formato padrão de erro da API. */
public record ApiError(Instant timestamp, int status, String error, String message, String path, List<String> details) {
}
