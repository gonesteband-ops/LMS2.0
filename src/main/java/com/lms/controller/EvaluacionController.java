package com.lms.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lms.dto.EvaluacionRequest;
import com.lms.model.Evaluacion;
import com.lms.model.Pregunta;
import com.lms.model.IntentoEvaluacion;
import com.lms.service.EvaluacionService;
import com.lms.service.InscripcionService;
import com.lms.service.IntentoEvaluacionService;
import com.lms.util.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Controlador REST de evaluaciones y preguntas.
 */
@RestController
@RequestMapping("/api/v1/cursos/{cursoId}/evaluaciones")
public class EvaluacionController {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(EvaluacionController.class);

    private final EvaluacionService evaluacionService;
    private final InscripcionService inscripcionService;
    private final IntentoEvaluacionService intentoService;
    private final SecurityUtils securityUtils;

    public EvaluacionController(EvaluacionService evaluacionService,
                                InscripcionService inscripcionService,
                                IntentoEvaluacionService intentoService,
                                SecurityUtils securityUtils) {
        this.evaluacionService = evaluacionService;
        this.inscripcionService = inscripcionService;
        this.intentoService = intentoService;
        this.securityUtils = securityUtils;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    public ResponseEntity<?> listar(@PathVariable UUID cursoId, Authentication authentication) {
        UUID usuarioId = securityUtils.getCurrentUserId(authentication);
        String role = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a.startsWith("ROLE_"))
                .findFirst().orElse("");

        System.out.println("[DEBUG EvaluacionController.listar] cursoId=" + cursoId
                + ", usuarioId=" + usuarioId + ", role=" + role);

        List<Evaluacion> evaluaciones = evaluacionService.listarPorCurso(cursoId);

        System.out.println("[DEBUG EvaluacionController.listar] evaluaciones count="
                + (evaluaciones != null ? evaluaciones.size() : "null"));

        // Si es estudiante, limpiar respuestas correctas y ocultar opciones correctas
        if ("ROLE_ESTUDIANTE".equals(role)) {
            ObjectMapper mapper = new ObjectMapper();
            for (Evaluacion ev : evaluaciones) {
                if (ev.getPreguntas() != null) {
                    for (Pregunta p : ev.getPreguntas()) {
                        p.setRespuestaCorrecta(null);
                        // También limpiar el flag 'correcta' dentro del JSON de opciones
                        if (p.getOpciones() != null && !p.getOpciones().isEmpty()) {
                            try {
                                List<Map<String, Object>> opcionesList = mapper.readValue(
                                        p.getOpciones(),
                                        new TypeReference<List<Map<String, Object>>>() {});
                                boolean modified = false;
                                for (Map<String, Object> opt : opcionesList) {
                                    if (opt.containsKey("correcta") && Boolean.TRUE.equals(opt.get("correcta"))) {
                                        opt.put("correcta", false);
                                        modified = true;
                                    }
                                }
                                if (modified) {
                                    p.setOpciones(mapper.writeValueAsString(opcionesList));
                                }
                            } catch (JsonProcessingException e) {
                                System.err.println("[ERROR] No se pudo parsear opciones de pregunta " + p.getId() + ": " + e.getMessage());
                            }
                        }
                    }
                }
            }
        }

        return ResponseEntity.ok(evaluaciones);
    }

    @GetMapping("/paged")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'INSTRUCTOR')")
    public ResponseEntity<Page<Evaluacion>> listarPaginado(@PathVariable UUID cursoId,
                                                           @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(evaluacionService.listarPorCursoPaginado(cursoId, pageable));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'INSTRUCTOR')")
    public ResponseEntity<Evaluacion> crear(@PathVariable UUID cursoId,
                                            @Valid @RequestBody EvaluacionRequest request,
                                            Authentication authentication) {
        UUID autorId = securityUtils.getCurrentUserId(authentication);
        return ResponseEntity.ok(evaluacionService.crearEvaluacion(cursoId, request, autorId));
    }

    @PutMapping("/{evaluacionId}")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'INSTRUCTOR')")
    public ResponseEntity<Evaluacion> actualizar(@PathVariable UUID cursoId,
                                                  @PathVariable UUID evaluacionId,
                                                  @Valid @RequestBody EvaluacionRequest request,
                                                  Authentication authentication) {
        UUID autorId = securityUtils.getCurrentUserId(authentication);
        return ResponseEntity.ok(evaluacionService.actualizarEvaluacion(evaluacionId, request, autorId));
    }

    @DeleteMapping("/{evaluacionId}")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'INSTRUCTOR')")
    public ResponseEntity<Map<String, String>> eliminar(@PathVariable UUID cursoId,
                                                         @PathVariable UUID evaluacionId,
                                                         Authentication authentication) {
        UUID autorId = securityUtils.getCurrentUserId(authentication);
        evaluacionService.eliminarEvaluacion(evaluacionId, autorId);
        return ResponseEntity.ok(Map.of("mensaje", "Evaluación eliminada correctamente"));
    }

    @GetMapping("/{evaluacionId}/preguntas")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'INSTRUCTOR')")
    public ResponseEntity<List<Pregunta>> preguntas(@PathVariable UUID evaluacionId) {
        return ResponseEntity.ok(evaluacionService.obtenerPreguntas(evaluacionId));
    }

    /**
     * Endpoint para estudiantes: lista evaluaciones del curso SÓLO si el estudiante está inscrito.
     */
    @GetMapping("/estudiante")
    @PreAuthorize("hasRole('ESTUDIANTE')")
    public ResponseEntity<List<Evaluacion>> listarParaEstudiante(
            @PathVariable UUID cursoId,
            Authentication authentication) {
        UUID usuarioId = securityUtils.getCurrentUserId(authentication);

        // Verificar inscripción
        if (!inscripcionService.isUsuarioInscritoEnCurso(usuarioId, cursoId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        // Retornar evaluaciones sin respuestas correctas (solo estructura)
        List<Evaluacion> evaluaciones = evaluacionService.listarPorCurso(cursoId);

        // Limpiar respuestas correctas de las preguntas para que el estudiante no las vea
        for (Evaluacion ev : evaluaciones) {
            if (ev.getPreguntas() != null) {
                for (Pregunta p : ev.getPreguntas()) {
                    p.setRespuestaCorrecta(null); // Ocultar respuesta correcta
                }
            }
        }

        return ResponseEntity.ok(evaluaciones);
    }

    /**
     * Endpoint para estudiante: obtiene preguntas de evaluación (sin respuestas correctas).
     */
    @GetMapping("/resultados")
    @PreAuthorize("hasRole('ESTUDIANTE')")
    public ResponseEntity<List<Map<String, Object>>> resultadosPorCurso(
            @PathVariable UUID cursoId,
            Authentication authentication) {
        UUID usuarioId = securityUtils.getCurrentUserId(authentication);
        List<Evaluacion> evaluaciones = evaluacionService.listarPorCurso(cursoId);
        List<Map<String, Object>> resultados = new java.util.ArrayList<>();
        for (Evaluacion ev : evaluaciones) {
            java.util.Optional<IntentoEvaluacion> intento = intentoService.buscarIntentoCompletado(ev.getId(), usuarioId);
            if (intento.isPresent()) {
                IntentoEvaluacion i = intento.get();
                Map<String, Object> r = new java.util.HashMap<>();
                r.put("evaluacionId", ev.getId().toString());
                r.put("nota", i.getNota());
                r.put("aprobado", i.getAprobado());
                r.put("completado", true);
                resultados.add(r);
            }
        }
        return ResponseEntity.ok(resultados);
    }

    @GetMapping("/{evaluacionId}/resultado-detalle")
    @PreAuthorize("hasRole('ESTUDIANTE')")
    public ResponseEntity<Map<String, Object>> resultadoDetalle(
            @PathVariable UUID cursoId,
            @PathVariable UUID evaluacionId,
            Authentication authentication) {
        UUID usuarioId = securityUtils.getCurrentUserId(authentication);

        if (!inscripcionService.isUsuarioInscritoEnCurso(usuarioId, cursoId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        try {
            java.util.Optional<IntentoEvaluacion> optIntento = intentoService.buscarIntentoCompletado(evaluacionId, usuarioId);
            if (optIntento.isEmpty()) {
                return ResponseEntity.ok(Map.of("resultado", "pendiente"));
            }
            IntentoEvaluacion i = optIntento.get();

            Map<String, Object> result = new java.util.HashMap<>();
            result.put("nota", i.getNota());
            result.put("aprobado", i.getAprobado());
            result.put("mensaje", i.getAprobado() ? "Aprobaste la evaluacion" : "No alcanzaste la nota minima");
            Map<String, Object> respData = i.getRespuestas();
            result.put("detalle", respData != null ? respData.get("detalle") : java.util.List.of());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("resultado", "error", "mensaje", e.getMessage()));
        }
    }

    @GetMapping("/{evaluacionId}/rendir")
    @PreAuthorize("hasRole('ESTUDIANTE')")
    public ResponseEntity<Map<String, Object>> rendirEvaluacion(
            @PathVariable UUID cursoId,
            @PathVariable UUID evaluacionId,
            Authentication authentication) {
        UUID usuarioId = securityUtils.getCurrentUserId(authentication);

        // Verificar inscripción en el curso
        if (!inscripcionService.isUsuarioInscritoEnCurso(usuarioId, cursoId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "No estás inscrito en este curso"));
        }

        // Obtener evaluación completa
        Evaluacion evaluacion = evaluacionService.buscarPorId(evaluacionId)
                .orElseThrow(() -> new IllegalArgumentException("Evaluación no encontrada"));

        // Verificar que la evaluación pertenezca al curso
        if (!evaluacion.getCurso().getId().equals(cursoId)) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "La evaluación no pertenece al curso especificado"));
        }

        // Obtener preguntas sin respuestas correctas
        List<Pregunta> preguntas = evaluacionService.obtenerPreguntas(evaluacionId);
        List<Map<String, Object>> preguntasParaEstudiante = new ArrayList<>();
        ObjectMapper mapper = new ObjectMapper();
        for (Pregunta p : preguntas) {
            Map<String, Object> pMap = new LinkedHashMap<>();
            pMap.put("id", p.getId());
            pMap.put("tipo", p.getTipo());
            pMap.put("contenido", p.getContenido());
            pMap.put("peso", p.getPeso());
            pMap.put("orden", p.getOrden());
            
            // Procesar opciones: parsear JSON string y sanitizar flag 'correcta' para estudiantes
            String rawOpciones = p.getOpciones();
            if (rawOpciones != null && !rawOpciones.isEmpty()) {
                try {
                    List<Map<String, Object>> opcionesList = mapper.readValue(
                            rawOpciones,
                            new TypeReference<List<Map<String, Object>>>() {});
                    // Sanitizar: ocultar flag 'correcta' al estudiante
                    for (Map<String, Object> opt : opcionesList) {
                        opt.remove("correcta");
                    }
                    pMap.put("opciones", opcionesList); // Enviar como array JSON, no como string
                } catch (JsonProcessingException e) {
                    System.err.println("[Rendir Error] No se pudo parsear opciones de pregunta " + p.getId() + ": " + e.getMessage());
                    pMap.put("opciones", rawOpciones); // Fallback: enviar como string
                }
            } else {
                pMap.put("opciones", rawOpciones);
            }
            
            pMap.put("tiempoRespuestaMs", p.getTiempoRespuestaMs());
            // NO incluir respuestaCorrecta
            preguntasParaEstudiante.add(pMap);
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("evaluacion", Map.of(
            "id", evaluacion.getId(),
            "titulo", evaluacion.getTitulo(),
            "descripcion", evaluacion.getDescripcion(),
            "pesoTotal", evaluacion.getPesoTotal(),
            "tiempoEstimadoMinutos", evaluacion.getTiempoEstimadoMinutos(),
            "configuracion", evaluacion.getConfiguracion()
        ));
        response.put("preguntas", preguntasParaEstudiante);

        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint para estudiante: envía respuestas de una evaluación.
     */
    @PostMapping("/{evaluacionId}/iniciar")
    @PreAuthorize("hasRole('ESTUDIANTE')")
    public ResponseEntity<Map<String, Object>> iniciarIntento(
            @PathVariable UUID cursoId,
            @PathVariable UUID evaluacionId,
            Authentication authentication) {
        UUID usuarioId = securityUtils.getCurrentUserId(authentication);

        if (!inscripcionService.isUsuarioInscritoEnCurso(usuarioId, cursoId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        try {
            IntentoEvaluacion intento = intentoService.iniciarIntento(evaluacionId, usuarioId);
            return ResponseEntity.ok(Map.of(
                "intentoId", intento.getId().toString(),
                "estado", intento.getEstado()
            ));
        } catch (IllegalStateException e) {
            log.warn("Error al iniciar intento: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "resultado", "error",
                "mensaje", e.getMessage()
            ));
        }
    }

    @PostMapping("/{evaluacionId}/responder")
    @PreAuthorize("hasRole('ESTUDIANTE')")
    public ResponseEntity<Map<String, Object>> responderEvaluacion(
            @PathVariable UUID cursoId,
            @PathVariable UUID evaluacionId,
            @RequestBody Map<String, Object> respuestas,
            Authentication authentication) {
        UUID usuarioId = securityUtils.getCurrentUserId(authentication);

        if (!inscripcionService.isUsuarioInscritoEnCurso(usuarioId, cursoId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        try {
            // Buscar intento EN_CURSO existente o crear uno nuevo
            IntentoEvaluacion intento = intentoService.buscarOCrearIntentoEnCurso(evaluacionId, usuarioId);
            intento = intentoService.entregarIntento(intento.getId(), respuestas);

            return ResponseEntity.ok(Map.of(
                "resultado", "success",
                "nota", intento.getNota(),
                "aprobado", intento.getAprobado(),
                "mensaje", intento.getAprobado()
                        ? "Aprobaste la evaluacion"
                        : "No alcanzaste la nota minima"
            ));
        } catch (IllegalStateException | IllegalArgumentException e) {
            log.warn("Error al responder evaluacion: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "resultado", "error",
                "mensaje", e.getMessage()
            ));
        }
    }

    @PostMapping("/{evaluacionId}/reiniciar")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'INSTRUCTOR')")
    public ResponseEntity<Map<String, Object>> reiniciarIntento(
            @PathVariable UUID cursoId,
            @PathVariable UUID evaluacionId,
            @RequestBody Map<String, String> body,
            Authentication authentication) {
        UUID adminUserId = securityUtils.getCurrentUserId(authentication);
        String motivo = body.getOrDefault("motivo", "Reinicio autorizado por administrador");

        try {
            // Necesitamos usuarioId a quien reiniciar - lo pasamos en el body
            String usuarioIdStr = body.get("usuarioId");
            if (usuarioIdStr == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Se requiere usuarioId"));
            }
            UUID usuarioId = UUID.fromString(usuarioIdStr);

            intentoService.reiniciarIntento(evaluacionId, usuarioId, adminUserId, motivo);
            return ResponseEntity.ok(Map.of(
                "resultado", "success",
                "mensaje", "Intento reiniciado correctamente"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "resultado", "error",
                "mensaje", e.getMessage()
            ));
        }
    }
}