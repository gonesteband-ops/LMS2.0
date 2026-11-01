package com.lms.controller;

import com.lms.model.EventoAuditoria;
import com.lms.repository.EventoAuditoriaRepository;
import com.lms.util.AuditCategoria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Controlador REST de auditoría (Event Sourcing - SOLO LECTURA).
 * <p>
 * Los eventos SOLO se crean via AuditLogger (INSERT-only).
 */
@RestController
@RequestMapping("/api/v1/auditoria")
@PreAuthorize("hasAnyRole('SUPERADMIN')")
public class AuditoriaController {

    private final EventoAuditoriaRepository auditoriaRepository;

    public AuditoriaController(EventoAuditoriaRepository auditoriaRepository) {
        this.auditoriaRepository = auditoriaRepository;
    }

    @GetMapping
    public ResponseEntity<Page<EventoAuditoria>> listar(@PageableDefault(size = 50) Pageable pageable) {
        return ResponseEntity.ok(auditoriaRepository.findAllOrderByCreatedAtDesc(pageable));
    }

    @GetMapping("/tipo/{tipoEvento}")
    public ResponseEntity<Page<EventoAuditoria>> porTipo(@PathVariable String tipoEvento,
                                                         @PageableDefault(size = 50) Pageable pageable) {
        return ResponseEntity.ok(auditoriaRepository.findByTipoEventoOrderByCreatedAtDesc(tipoEvento, pageable));
    }

    @GetMapping("/actor/{actorId}")
    public ResponseEntity<Page<EventoAuditoria>> porActor(@PathVariable UUID actorId,
                                                          @PageableDefault(size = 50) Pageable pageable) {
        return ResponseEntity.ok(auditoriaRepository.findByActorIdOrderByCreatedAtDesc(actorId, pageable));
    }

    /**
     * Lista las categorías disponibles para el filtro jerárquico.
     * Ej: USUARIOS, CURSOS, EVALUACIONES, etc.
     */
    @GetMapping("/categorias")
    public ResponseEntity<List<Map<String, Object>>> listarCategorias() {
        return ResponseEntity.ok(AuditCategoria.listarCategorias());
    }

    /**
     * Lista los tipos de evento dentro de una categoría.
     * Ej: categoria=USUARIOS → [USUARIO_REGISTRADO, USUARIO_CREADO_ADMIN, ...]
     */
    @GetMapping("/categorias/{categoriaId}/tipos")
    public ResponseEntity<List<Map<String, Object>>> listarTiposPorCategoria(
            @PathVariable String categoriaId) {
        return ResponseEntity.ok(AuditCategoria.listarTiposPorCategoria(categoriaId));
    }
}
