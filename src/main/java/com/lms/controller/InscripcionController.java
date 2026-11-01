package com.lms.controller;

import com.lms.dto.CompletarCursoRequest;
import com.lms.dto.InscripcionMasivaReport;
import com.lms.dto.InscripcionRequest;
import com.lms.dto.InscripcionResponse;
import com.lms.service.InscripcionService;
import com.lms.util.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Controlador de inscripciones (participación de usuarios en cursos).
 * <p>
 * Expone endpoints REST para operaciones CRUD y endpoints HTMX
 * para fragmentos HTML en el frontend.
 */
@RestController
@RequestMapping("/api/v1")
public class InscripcionController {

    private final InscripcionService inscripcionService;
    private final SecurityUtils securityUtils;

    public InscripcionController(InscripcionService inscripcionService, SecurityUtils securityUtils) {
        this.inscripcionService = inscripcionService;
        this.securityUtils = securityUtils;
    }

    /**
     * Inscribe un usuario en un curso (manual 1-to-1).
     * Acepta UUID, RUT o email como identificador del usuario.
     */
    @PostMapping("/cursos/{cursoId}/inscribir")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'INSTRUCTOR')")
    public ResponseEntity<InscripcionResponse> inscribir(
            @PathVariable UUID cursoId,
            @Valid @RequestBody InscripcionRequest request,
            HttpServletRequest httpRequest) {
        UUID usuarioId = inscripcionService.resolverIdentificador(request.usuarioId());
        String ipCliente = httpRequest.getRemoteAddr();
        LocalDateTime timestamp = LocalDateTime.now();
        return ResponseEntity.ok(inscripcionService.inscribirUsuarioConFirma(
                cursoId, usuarioId, request.declaracionJuradaAceptada(), ipCliente, timestamp));
    }

    /**
     * Inscripción masiva via archivo CSV/XLSX.
     */
    @PostMapping(value = "/cursos/{cursoId}/inscribir-masivo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('SUPERADMIN')")
    public ResponseEntity<InscripcionMasivaReport> inscribirMasivo(
            @PathVariable UUID cursoId,
            @RequestParam("archivo") MultipartFile archivo) {
        InscripcionMasivaReport report = inscripcionService.inscribirMasivo(cursoId, archivo);
        return ResponseEntity.ok(report);
    }

    /**
     * Lista los cursos del usuario autenticado.
     */
    @GetMapping("/mis-cursos")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<InscripcionResponse>> listarMisCursos(Authentication authentication) {
        UUID usuarioId = securityUtils.getCurrentUserId(authentication);
        return ResponseEntity.ok(inscripcionService.listarMisCursos(usuarioId));
    }

    /**
     * Lista los cursos pendientes del usuario autenticado.
     */
    @GetMapping("/mis-cursos/pendientes")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<InscripcionResponse>> listarPendientes(Authentication authentication) {
        UUID usuarioId = securityUtils.getCurrentUserId(authentication);
        return ResponseEntity.ok(inscripcionService.listarPendientes(usuarioId));
    }

    /**
     * Lista los cursos completados del usuario autenticado.
     */
    @GetMapping("/mis-cursos/completados")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<InscripcionResponse>> listarCompletados(Authentication authentication) {
        UUID usuarioId = securityUtils.getCurrentUserId(authentication);
        return ResponseEntity.ok(inscripcionService.listarCompletados(usuarioId));
    }

    /**
     * Lista los participantes (inscripciones) de un curso específico.
     * Útil para administradores que gestionan un curso.
     */
    @GetMapping("/cursos/{cursoId}/participantes")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<InscripcionResponse>> listarParticipantes(@PathVariable UUID cursoId) {
        return ResponseEntity.ok(inscripcionService.listarParticipantesPorCurso(cursoId));
    }

    /**
     * Marca un curso como completado para el usuario autenticado.
     */
    @PostMapping("/cursos/{cursoId}/completar")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<InscripcionResponse> completarCurso(
            @PathVariable UUID cursoId,
            @RequestBody(required = false) CompletarCursoRequest request,
            Authentication authentication) {
        UUID usuarioId = securityUtils.getCurrentUserId(authentication);
        Double nota = request != null ? request.nota() : null;
        return ResponseEntity.ok(inscripcionService.completarCurso(cursoId, usuarioId, nota));
    }

    /**
     * Obtiene estadísticas de cursos del usuario.
     */
    @GetMapping("/mis-cursos/estadisticas")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Long>> obtenerEstadisticas(Authentication authentication) {
        UUID usuarioId = securityUtils.getCurrentUserId(authentication);
        return ResponseEntity.ok(inscripcionService.obtenerEstadisticas(usuarioId));
    }

    /**
     * Elimina (soft-delete) una inscripción.
     */
    @DeleteMapping("/inscripciones/{id}")
    @PreAuthorize("hasAnyRole('SUPERADMIN')")
    public ResponseEntity<Void> eliminarInscripcion(
            @PathVariable UUID id,
            @RequestParam String motivo,
            Authentication authentication) {
        UUID eliminadoPor = securityUtils.getCurrentUserId(authentication);
        inscripcionService.eliminarInscripcion(id, eliminadoPor, motivo);
        return ResponseEntity.noContent().build();
    }

    /**
     * Forza la completación de una inscripción por un administrador.
     */
    @PostMapping("/inscripciones/{id}/forzar-completado")
    @PreAuthorize("hasAnyRole('SUPERADMIN')")
    public ResponseEntity<InscripcionResponse> forzarCompletado(
            @PathVariable UUID id,
            @RequestBody Map<String, String> requestBody,
            Authentication authentication) {
        // Validar que motivo no sea nulo ni vacío
        String motivo = requestBody.get("motivo");
        if (motivo == null || motivo.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        
        String comentario = requestBody.get("comentario");
        UUID adminUserId = securityUtils.getCurrentUserId(authentication);
        InscripcionResponse response = inscripcionService.forzarCompletado(id, adminUserId, motivo, comentario);
        return ResponseEntity.ok(response);
    }
}

/**
 * Controlador HTMX para fragmentos del perfil del usuario.
 */
@Controller
@RequestMapping("/hx/perfil")
class PerfilHtmxController {

    private final InscripcionService inscripcionService;
    private final SecurityUtils securityUtils;

    public PerfilHtmxController(InscripcionService inscripcionService, SecurityUtils securityUtils) {
        this.inscripcionService = inscripcionService;
        this.securityUtils = securityUtils;
    }

    /**
     * Fragmento HTMX que muestra los cursos del usuario en el perfil.
     */
    @GetMapping("/cursos")
    public String cursosFragment(Model model, Authentication authentication) {
        UUID usuarioId = securityUtils.getCurrentUserId(authentication);
        List<InscripcionResponse> pendientes = inscripcionService.listarPendientes(usuarioId);
        List<InscripcionResponse> completados = inscripcionService.listarCompletados(usuarioId);
        Map<String, Long> stats = inscripcionService.obtenerEstadisticas(usuarioId);

        model.addAttribute("pendientes", pendientes);
        model.addAttribute("completados", completados);
        model.addAttribute("stats", stats);
        return "dashboard/perfil-cursos :: cursos";
    }

    
}
// force recompile 1
