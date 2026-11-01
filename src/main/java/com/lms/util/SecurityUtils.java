package com.lms.util;

import com.lms.model.Usuario;
import com.lms.repository.UsuarioRepository;
import com.lms.security.UserPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Utilidad para extraer información del usuario autenticado
 * desde el contexto de seguridad de Spring.
 */
@Component
public class SecurityUtils {

    private final UsuarioRepository usuarioRepository;

    public SecurityUtils(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    /**
     * Obtiene el UUID del usuario autenticado.
     */
    public UUID getCurrentUserId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalArgumentException("Usuario no autenticado");
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof UserPrincipal userPrincipal) {
            return userPrincipal.getUserId();
        }

        // Fallback: buscar por email
        String email = authentication.getName();
        return usuarioRepository.findByEmailAndDeletedAtIsNull(email)
                .map(Usuario::getId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + email));
    }

    /**
     * Obtiene el UserPrincipal completo.
     */
    public UserPrincipal getCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalArgumentException("Usuario no autenticado");
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof UserPrincipal userPrincipal) {
            return userPrincipal;
        }

        throw new IllegalArgumentException("Principal no es UserPrincipal: " + principal.getClass());
    }
}
