package com.lms.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * DTO para registro de firma electrónica (Ley 19.799).
 * La IP de origen se extrae del HttpServletRequest, no del DTO.
 */
public record FirmaRequest(
        @NotNull(message = "El ID del documento es obligatorio")
        UUID documentoId,

        String sistemaOperativo,

        String geolocalizacion
) {}
