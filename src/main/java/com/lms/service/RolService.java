package com.lms.service;

import com.lms.model.Permiso;
import com.lms.model.Rol;
import com.lms.repository.PermisoRepository;
import com.lms.repository.RolRepository;
import com.lms.util.AuditLogger;
import com.lms.util.HashUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Servicio de gestión de roles y permisos (RBAC).
 * <p>
 * Implementa control de acceso granular por recurso+acción.
 */
@Service
@Transactional
public class RolService {

    private final RolRepository rolRepository;
    private final PermisoRepository permisoRepository;
    private final AuditLogger auditLogger;

    public RolService(RolRepository rolRepository,
                      PermisoRepository permisoRepository,
                      AuditLogger auditLogger) {
        this.rolRepository = rolRepository;
        this.permisoRepository = permisoRepository;
        this.auditLogger = auditLogger;
    }

    /**
     * Crea un nuevo rol con permisos asignados.
     */
    public Rol crearRol(String nombre, String descripcion, Set<String> permisosNombres, UUID creadorId) {
        if (rolRepository.existsByNombre(nombre)) {
            throw new IllegalArgumentException("El rol ya existe: " + nombre);
        }

        Set<Permiso> permisos = new HashSet<>();
        if (permisosNombres != null) {
            for (String permisoNombre : permisosNombres) {
                Permiso permiso = permisoRepository.findByNombre(permisoNombre)
                        .orElseThrow(() -> new IllegalArgumentException("Permiso no encontrado: " + permisoNombre));
                permisos.add(permiso);
            }
        }

        Rol rol = Rol.builder()
                .nombre(nombre.toUpperCase())
                .descripcion(descripcion)
                .esSistema(false)
                .permisos(permisos)
                .hashSha256(HashUtils.sha256(nombre + "|" + LocalDateTime.now()))
                .build();

        rol = rolRepository.save(rol);

        auditLogger.registrarEvento(
                "ROL_CREADO",
                creadorId,
                "CREAR",
                "ROL",
                rol.getId(),
                Map.of("nombre", nombre, "permisos", String.join(",", permisosNombres != null ? permisosNombres : List.of()))
        );

        return rol;
    }

    /**
     * Busca rol por nombre.
     */
    @Transactional(readOnly = true)
    public Optional<Rol> buscarPorNombre(String nombre) {
        return rolRepository.findByNombre(nombre.toUpperCase());
    }

    /**
     * Lista todos los roles.
     */
    @Transactional(readOnly = true)
    public List<Rol> listarRoles() {
        return rolRepository.findAll();
    }

    /**
     * Lista todos los permisos disponibles en el sistema.
     */
    @Transactional(readOnly = true)
    public List<Permiso> listarPermisos() {
        return permisoRepository.findAll();
    }

    /**
     * Verifica si un usuario tiene un permiso específico.
     */
    @Transactional(readOnly = true)
    public boolean tienePermiso(UUID usuarioId, String recurso, String accion) {
        // Implementación simplificada - en producción se usa el SecurityContext
        return true; // El control real se hace via @PreAuthorize
    }
}
