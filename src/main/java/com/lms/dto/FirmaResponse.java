package com.lms.dto;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO para respuesta de firma electrónica.
 */
public record FirmaResponse(
        UUID id,
        UUID usuarioId,
        UUID documentoId,
        String hashDocumento,
        String hashSha256,
        String hashAnterior,
        LocalDateTime timestamp
) {}
