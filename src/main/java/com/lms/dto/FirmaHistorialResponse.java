package com.lms.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record FirmaHistorialResponse(
        UUID id,
        String codigoDocumento,
        String nombreDocumento,
        LocalDateTime fechaFirma,
        String ipOrigen,
        String hashSha256
) {}
