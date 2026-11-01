package com.lms.controller;

import com.lms.model.Usuario;
import com.lms.service.UsuarioService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * Controlador REST de usuarios.
 * <p>
 * Endpoints para gestión de perfil y administración de usuarios.
 */
@RestController
@RequestMapping("/api/v1/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> perfil(Authentication authentication) {
        Usuario usuario = usuarioService.buscarPorEmail(authentication.getName())
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        return ResponseEntity.ok(Map.of(
                "id", usuario.getId(),
                "email", usuario.getEmail(),
                "nombreCompleto", usuario.getNombreCompleto(),
                "consentimientoOtorgado", usuario.isConsentimientoOtorgado(),
                "mfaHabilitado", usuario.isMfaHabilitado(),
                "roles", usuario.getRoles().stream().map(r -> "ROLE_" + r.getNombre()).toList(),
                "createdAt", usuario.getCreatedAt()
        ));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPERADMIN')")
    public ResponseEntity<Void> eliminar(@PathVariable UUID id,
                                         @RequestParam String motivo,
                                         Authentication authentication) {
        Usuario admin = usuarioService.buscarPorEmail(authentication.getName())
                .orElseThrow(() -> new IllegalArgumentException("Administrador no encontrado"));
        usuarioService.eliminar(id, admin.getId(), motivo);
        return ResponseEntity.noContent().build();
    }
}
