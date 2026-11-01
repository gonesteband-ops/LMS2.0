package com.lms.dto;

import java.util.Set;
import java.util.UUID;

/**
 * DTO para respuesta de autenticación (JWT tokens + datos de usuario).
 */
public record AuthResponse(
        UUID userId,
        String email,
        String nombreCompleto,
        Set<String> roles,
        String accessToken,
        String refreshToken,
        long expiresIn,
        boolean requiresMfa,
        boolean consentimientoPendiente
) {}
