package com.lms.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/**
 * DTO para verificación MFA TOTP.
 */
public record MfaVerificarRequest(
        @NotBlank(message = "El token temporal es obligatorio")
        String tokenTemporal,

        @Min(value = 100000, message = "El código debe tener 6 dígitos")
        @Max(value = 999999, message = "El código debe tener 6 dígitos")
        int codigo
) {}
