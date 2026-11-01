package com.lms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

/**
 * DTO para creación/actualización de preguntas.
 */
public record PreguntaRequest(
        @NotBlank(message = "El tipo de pregunta es obligatorio")
        String tipo,

        @NotBlank(message = "El contenido de la pregunta es obligatorio")
        String contenido,

        @Positive(message = "El peso debe ser positivo")
        double peso,

        int tiempoRespuestaMs,

        String opciones,

        String respuestaCorrecta,

        int orden
) {}
