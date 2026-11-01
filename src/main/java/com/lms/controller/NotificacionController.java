package com.lms.controller;

import com.lms.dto.PagedResponse;
import com.lms.model.Notificacion;
import com.lms.service.NotificacionService;
import com.lms.util.SecurityUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Controlador REST del sistema de notificaciones automáticas.
 * <p>
 * Expone endpoints para que el frontend (Thymeleaf + HTMX) consulte,
 * marque y administre las notificaciones del usuario autenticado.
 */
@RestController
@RequestMapping("/api/v1/notificaciones")
public class NotificacionController {

    private final NotificacionService notificacionService;
    private final SecurityUtils securityUtils;

    public NotificacionController(NotificacionService notificacionService, SecurityUtils securityUtils) {
        this.notificacionService = notificacionService;
        this.securityUtils = securityUtils;
    }

    /**
     * Lista paginada de notificaciones del usuario autenticado.
     * <p>
     * Ejemplo: GET /api/v1/notificaciones?page=0&size=10&sort=createdAt,desc
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PagedResponse<Notificacion>> listar(Pageable pageable, Authentication authentication) {
        UUID usuarioId = securityUtils.getCurrentUserId(authentication);
        Page<Notificacion> pagina = notificacionService.listar(usuarioId, pageable);
        PagedResponse<Notificacion> response = new PagedResponse<>(
                pagina.getContent(),
                pagina.getNumber(),
                pagina.getSize(),
                pagina.getTotalElements(),
                pagina.getTotalPages(),
                pagina.isFirst(),
                pagina.isLast()
        );
        return ResponseEntity.ok(response);
    }

    /**
     * Lista las notificaciones no leídas del usuario autenticado.
     * Útil para el dropdown del header.
     */
    @GetMapping("/no-leidas")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Notificacion>> listarNoLeidas(Authentication authentication) {
        UUID usuarioId = securityUtils.getCurrentUserId(authentication);
        return ResponseEntity.ok(notificacionService.listarNoLeidas(usuarioId));
    }

    /**
     * Devuelve el número de notificaciones no leídas.
     */
    @GetMapping("/contar")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Long>> contarNoLeidas(Authentication authentication) {
        UUID usuarioId = securityUtils.getCurrentUserId(authentication);
        long count = notificacionService.contarNoLeidas(usuarioId);
        return ResponseEntity.ok(Map.of("noLeidas", count));
    }

    /**
     * Marca una notificación específica como leída.
     */
    @PostMapping("/{id}/leer")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> marcarComoLeida(@PathVariable UUID id) {
        notificacionService.marcarComoLeida(id);
        return ResponseEntity.ok().build();
    }

    /**
     * Marca todas las notificaciones del usuario autenticado como leídas.
     */
    @PostMapping("/leer-todas")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> marcarTodasComoLeidas(Authentication authentication) {
        UUID usuarioId = securityUtils.getCurrentUserId(authentication);
        int actualizadas = notificacionService.marcarTodasComoLeidas(usuarioId);
        return ResponseEntity.ok(Map.of(
                "actualizadas", actualizadas,
                "mensaje", actualizadas + " notificaciones marcadas como leídas"
        ));
    }

    /**
     * Limpia notificaciones leídas con más de 30 días de antigüedad.
     * Solo accesible para administradores.
     */
    @DeleteMapping("/limpiar")
    @PreAuthorize("hasAnyRole('SUPERADMIN')")
    public ResponseEntity<Map<String, Object>> limpiarNotificacionesAntiguas() {
        int eliminadas = notificacionService.limpiarNotificacionesAntiguas();
        return ResponseEntity.ok(Map.of(
                "eliminadas", eliminadas,
                "mensaje", eliminadas + " notificaciones antiguas eliminadas"
        ));
    }
}
