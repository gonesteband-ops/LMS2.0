package com.lms.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO estandarizado para respuestas de error.
 */
public record ErrorResponse(
        int status,
        String error,
        String message,
        String path,
        LocalDateTime timestamp,
        List<String> details
) {}
