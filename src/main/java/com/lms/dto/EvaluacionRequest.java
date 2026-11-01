package com.lms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

import java.util.List;

/**
 * DTO para creación/actualización de evaluaciones.
 */
public record EvaluacionRequest(
        @NotBlank(message = "El título es obligatorio")
        String titulo,

        String descripcion,

        @Positive(message = "El peso total debe ser positivo")
        double pesoTotal,

        int tiempoEstimadoMinutos,

        String configuracion,

        Double notaMinima,

        List<PreguntaRequest> preguntas
) {}
