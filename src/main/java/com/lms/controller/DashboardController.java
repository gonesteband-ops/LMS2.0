package com.lms.controller;

import com.lms.model.Curso;
import com.lms.model.Evaluacion;
import com.lms.model.EventoAuditoria;
import com.lms.model.Inscripcion;
import com.lms.repository.CursoRepository;
import com.lms.repository.DocumentoRepository;
import com.lms.repository.EvaluacionRepository;
import com.lms.repository.EventoAuditoriaRepository;
import com.lms.repository.ExportacionRepository;
import com.lms.repository.FirmaElectronicaRepository;
import com.lms.repository.InscripcionRepository;
import com.lms.repository.IntentoEvaluacionRepository;
import com.lms.util.SecurityUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Controlador REST del Dashboard.
 * Provee los datos en tiempo real para los KPIs y la actividad reciente.
 */
@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {

    private final CursoRepository cursoRepository;
    private final DocumentoRepository documentoRepository;
    private final FirmaElectronicaRepository firmaRepository;
    private final ExportacionRepository exportacionRepository;
    private final EventoAuditoriaRepository eventoAuditoriaRepository;
    private final InscripcionRepository inscripcionRepository;
    private final IntentoEvaluacionRepository intentoEvaluacionRepository;
    private final EvaluacionRepository evaluacionRepository;
    private final SecurityUtils securityUtils;

    public DashboardController(CursoRepository cursoRepository,
                               DocumentoRepository documentoRepository,
                               FirmaElectronicaRepository firmaRepository,
                               ExportacionRepository exportacionRepository,
                               EventoAuditoriaRepository eventoAuditoriaRepository,
                               InscripcionRepository inscripcionRepository,
                               IntentoEvaluacionRepository intentoEvaluacionRepository,
                               EvaluacionRepository evaluacionRepository,
                               SecurityUtils securityUtils) {
        this.cursoRepository = cursoRepository;
        this.documentoRepository = documentoRepository;
        this.firmaRepository = firmaRepository;
        this.exportacionRepository = exportacionRepository;
        this.eventoAuditoriaRepository = eventoAuditoriaRepository;
        this.inscripcionRepository = inscripcionRepository;
        this.intentoEvaluacionRepository = intentoEvaluacionRepository;
        this.evaluacionRepository = evaluacionRepository;
        this.securityUtils = securityUtils;
    }

    /**
     * GET /api/v1/dashboard/resumen
     * Devuelve los contadores en tiempo real para los KPIs del dashboard.
     */
    @GetMapping("/resumen")
    public ResponseEntity<Map<String, Object>> obtenerResumen(Authentication authentication) {
        UUID usuarioId = securityUtils.getCurrentUserId(authentication);

        // Determinar si el usuario es staff (ADMIN/INSTRUCTOR/SUPERADMIN)
        boolean esStaff = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_SUPERADMIN")
                            || a.getAuthority().equals("ROLE_INSTRUCTOR")
                            || a.getAuthority().equals("ROLE_SUPERADMIN"));

        long cursosActivos;
        if (esStaff) {
            cursosActivos = cursoRepository.countByDeletedAtIsNull();
        } else {
            cursosActivos = inscripcionRepository.countByUsuarioIdAndDeletedAtIsNull(usuarioId);
        }

        long documentos;
        if (esStaff) {
            documentos = documentoRepository.countByDeletedAtIsNull();
        } else {
            documentos = documentoRepository.findByUsuarioInscripciones(usuarioId).size();
        }

        long firmas = firmaRepository.countByUsuarioId(usuarioId);

        long evaluacionesPendientes;
        if (esStaff) {
            evaluacionesPendientes = intentoEvaluacionRepository.countByUsuarioIdAndEstadoNotAndDeletedAtIsNull(usuarioId, "COMPLETADO");
        } else {
            List<Inscripcion> inscripciones = inscripcionRepository.findByUsuarioIdAndDeletedAtIsNull(usuarioId);
            evaluacionesPendientes = 0;
            for (Inscripcion insc : inscripciones) {
                Curso curso = insc.getCurso();
                List<Evaluacion> evaluaciones = evaluacionRepository.findByCursoId(curso.getId());
                for (Evaluacion ev : evaluaciones) {
                    long completados = intentoEvaluacionRepository
                            .countByEvaluacionIdAndUsuarioIdAndEstadoAndDeletedAtIsNull(ev.getId(), usuarioId, "COMPLETADO");
                    if (completados == 0) {
                        evaluacionesPendientes++;
                    }
                }
            }
        }

        long exportaciones = exportacionRepository.countBySolicitanteId(usuarioId);

        Map<String, Object> resumen = Map.of(
                "cursosActivos", cursosActivos,
                "documentos", documentos,
                "firmas", firmas,
                "exportaciones", exportaciones,
                "evaluacionesPendientes", evaluacionesPendientes
        );

        return ResponseEntity.ok(resumen);
    }

    /**
     * GET /api/v1/dashboard/evaluaciones-pendientes
     * Devuelve las evaluaciones pendientes (no completadas) del estudiante
     * a través de todos sus cursos inscritos.
     */
    @GetMapping("/evaluaciones-pendientes")
    @Transactional(readOnly = true)
    public ResponseEntity<List<Map<String, Object>>> obtenerEvaluacionesPendientes(Authentication authentication) {
        UUID usuarioId = securityUtils.getCurrentUserId(authentication);

        List<Inscripcion> inscripciones = inscripcionRepository.findByUsuarioIdAndDeletedAtIsNull(usuarioId);
        List<Map<String, Object>> pendientes = new ArrayList<>();

        for (Inscripcion insc : inscripciones) {
            Curso curso = insc.getCurso();
            List<Evaluacion> evaluaciones = evaluacionRepository.findByCursoId(curso.getId());

            for (Evaluacion ev : evaluaciones) {
                long completados = intentoEvaluacionRepository
                        .countByEvaluacionIdAndUsuarioIdAndEstadoAndDeletedAtIsNull(ev.getId(), usuarioId, "COMPLETADO");
                if (completados == 0) {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("cursoId", curso.getId().toString());
                    item.put("cursoTitulo", curso.getTitulo());
                    item.put("evaluacionId", ev.getId().toString());
                    item.put("evaluacionTitulo", ev.getTitulo());
                    item.put("codigo", ev.getCodigo());
                    item.put("pesoTotal", ev.getPesoTotal());
                    item.put("tiempoEstimadoMinutos", ev.getTiempoEstimadoMinutos());
                    item.put("preguntasCount", ev.getPreguntas() != null ? ev.getPreguntas().size() : 0);
                    pendientes.add(item);
                }
            }
        }

        return ResponseEntity.ok(pendientes);
    }

    /**
     * GET /api/v1/dashboard/actividad
     * Devuelve los últimos eventos de auditoría del usuario autenticado.
     */
    @GetMapping("/actividad")
    public ResponseEntity<List<EventoAuditoria>> obtenerActividad(Authentication authentication) {
        UUID usuarioId = securityUtils.getCurrentUserId(authentication);

        List<EventoAuditoria> eventos = eventoAuditoriaRepository
                .findByActorIdOrderByCreatedAtDesc(usuarioId, PageRequest.of(0, 10))
                .getContent();

        return ResponseEntity.ok(eventos);
    }
}
