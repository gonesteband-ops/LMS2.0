package com.lms.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.Map;

public record ConsentimientoRequest(
        @NotBlank(message = "La versión de la política es obligatoria")
        String versionPolitica,

        @NotBlank(message = "El idioma es obligatorio")
        String idioma,

        @NotEmpty(message = "Debe seleccionar al menos una finalidad")
        Map<String, Boolean> finalidades
) {
    @AssertTrue(message = "Debe aceptar la finalidad CUENTA para continuar")
    public boolean isFinalidadCuentaAceptada() {
        return finalidades != null && Boolean.TRUE.equals(finalidades.get("CUENTA"));
    }
}
