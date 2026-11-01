package com.lms.controller;

import com.lms.model.Rol;
import com.lms.service.RolService;
import com.lms.util.SecurityUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Controlador REST de roles y permisos (RBAC).
 */
@RestController
@RequestMapping("/api/v1/roles")
@PreAuthorize("hasAnyRole('SUPERADMIN')")
public class RolController {

    private final RolService rolService;
    private final SecurityUtils securityUtils;

    public RolController(RolService rolService, SecurityUtils securityUtils) {
        this.rolService = rolService;
        this.securityUtils = securityUtils;
    }

    @GetMapping
    public ResponseEntity<List<Rol>> listar() {
        return ResponseEntity.ok(rolService.listarRoles());
    }

    @PostMapping
    public ResponseEntity<Rol> crear(@RequestBody Map<String, Object> request,
                                     Authentication authentication) {
        String nombre = (String) request.get("nombre");
        String descripcion = (String) request.getOrDefault("descripcion", "");
        @SuppressWarnings("unchecked")
        Set<String> permisos = request.get("permisos") != null
                ? Set.of(((List<String>) request.get("permisos")).toArray(new String[0]))
                : Set.of();

        UUID creadorId = securityUtils.getCurrentUserId(authentication);
        return ResponseEntity.ok(rolService.crearRol(nombre, descripcion, permisos, creadorId));
    }
}
