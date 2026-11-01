package com.lms.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

/**
 * DTO para creación de usuarios desde el panel de administración.
 * Permite asignar un rol específico (a diferencia del registro público).
 */
public record CrearUsuarioRequest(
        @NotBlank(message = "El RUT es obligatorio")
        @Size(min = 8, max = 20, message = "El RUT debe tener entre 8 y 20 caracteres")
        String rut,

        @NotBlank(message = "El nombre completo es obligatorio")
        @Size(min = 3, max = 255, message = "El nombre debe tener entre 3 y 255 caracteres")
        String nombreCompleto,

        @NotBlank(message = "El email es obligatorio")
        @Email(message = "Formato de email inválido")
        String email,

        @NotBlank(message = "La contraseña es obligatoria")
        @Size(min = 12, message = "La contraseña debe tener al menos 12 caracteres")
        String password,

        @NotBlank(message = "Debe confirmar la contraseña")
        String confirmarPassword,

        @NotNull(message = "Debe seleccionar un rol")
        UUID rolId,

        @Past(message = "La fecha de nacimiento debe ser una fecha pasada")
        LocalDate fechaNacimiento
) {}
