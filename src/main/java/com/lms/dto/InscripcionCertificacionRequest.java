package com.lms.dto;

import jakarta.validation.constraints.NotBlank;

public record InscripcionCertificacionRequest(
    @NotBlank String usuarioId
) {}
