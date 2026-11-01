package com.lms.controller;

import com.lms.dto.CursoRequest;
import com.lms.model.Curso;
import com.lms.model.VersionCurso;
import com.lms.service.CursoService;
import com.lms.service.InscripcionService;
import com.lms.util.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controlador REST de cursos con versionado.
 */
@RestController
@RequestMapping("/api/v1/cursos")
public class CursoController {

    private final CursoService cursoService;
    private final InscripcionService inscripcionService;
    private final SecurityUtils securityUtils;

    public CursoController(CursoService cursoService,
                           InscripcionService inscripcionService,
                           SecurityUtils securityUtils) {
        this.cursoService = cursoService;
        this.inscripcionService = inscripcionService;
        this.securityUtils = securityUtils;
    }

    /**
     * Lista cursos en los que el usuario autenticado está inscrito.
     */
    @GetMapping("/mis-cursos")
    public ResponseEntity<List<com.lms.dto.InscripcionResponse>> misCursos(Authentication authentication) {
        UUID usuarioId = securityUtils.getCurrentUserId(authentication);
        return ResponseEntity.ok(inscripcionService.listarMisCursos(usuarioId));
    }

    @GetMapping
    public ResponseEntity<Page<Curso>> listar(@PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(cursoService.listarCursos(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Curso> obtener(@PathVariable UUID id) {
        return cursoService.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('CURSO_CREAR')")
    public ResponseEntity<Curso> crear(@Valid @RequestBody CursoRequest request,
                                       Authentication authentication) {
        UUID autorId = securityUtils.getCurrentUserId(authentication);
        return ResponseEntity.ok(cursoService.crearCurso(request, autorId));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'INSTRUCTOR')")
    public ResponseEntity<Curso> actualizar(@PathVariable UUID id,
                                            @Valid @RequestBody CursoRequest request,
                                            Authentication authentication) {
        UUID autorId = securityUtils.getCurrentUserId(authentication);
        return ResponseEntity.ok(cursoService.actualizarCurso(id, request, autorId));
    }

    @GetMapping("/{id}/versiones")
    public ResponseEntity<Page<VersionCurso>> versiones(@PathVariable UUID id,
                                                        @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(cursoService.obtenerVersiones(id, pageable));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPERADMIN')")
    public ResponseEntity<Void> eliminar(@PathVariable UUID id,
                                         @RequestParam String motivo,
                                         Authentication authentication) {
        UUID eliminadoPor = securityUtils.getCurrentUserId(authentication);
        cursoService.eliminarCurso(id, eliminadoPor, motivo);
        return ResponseEntity.noContent().build();
    }
}
