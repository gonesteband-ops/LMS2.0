package com.lms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import java.util.UUID;

public record PreguntaCertificacionRequest(
    UUID id,
    @NotBlank String tipo,
    @NotBlank String contenido,
    @Positive Double peso,
    String opciones,
    String respuestaCorrecta,
    String multimediaUrl,
    int orden
) {}
