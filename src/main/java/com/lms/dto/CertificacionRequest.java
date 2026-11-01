package com.lms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import java.util.List;
import java.util.Map;

public record CertificacionRequest(
    @NotBlank String titulo,
    String descripcion,
    @Positive Double notaMinima,
    Integer tiempoEstimadoMinutos,
    Map<String, Object> configuracion,
    @NotEmpty List<PreguntaCertificacionRequest> preguntas
) {}
