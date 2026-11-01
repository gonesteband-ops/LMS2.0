package com.lms.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO para solicitar inscripción de un usuario en un curso.
 * Acepta UUID, RUT o email como identificador del usuario.
 */
public record InscripcionRequest(
        @NotBlank(message = "El ID del usuario, RUT o email es obligatorio")
        String usuarioId,

        boolean declaracionJuradaAceptada
) {}
