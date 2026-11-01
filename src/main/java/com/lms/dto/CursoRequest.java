package com.lms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO para creación/actualización de cursos.
 */
public record CursoRequest(
        @NotBlank(message = "El título es obligatorio")
        @Size(min = 3, max = 255, message = "El título debe tener entre 3 y 255 caracteres")
        String titulo,

        @Size(max = 2000, message = "La descripción no debe exceder 2000 caracteres")
        String descripcion,

        @NotBlank(message = "La categoría es obligatoria")
        String categoria,

        String contenido,

        String motivoCambio,

        String fechaLimite,

        boolean requiereFirmaCumplimiento,

        Integer duracionEstimada,

        String estado
) {}
