package com.lms.service;

import com.lms.dto.EvaluacionRequest;
import com.lms.dto.PreguntaRequest;
import com.lms.model.Curso;
import com.lms.model.Evaluacion;
import com.lms.model.Pregunta;
import com.lms.repository.EvaluacionRepository;
import com.lms.repository.PreguntaRepository;
import com.lms.util.AuditLogger;
import com.lms.util.HashUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Servicio de gestión de evaluaciones y preguntas.
 */
@Service
@Transactional
public class EvaluacionService {

    private static final Logger log = LoggerFactory.getLogger(EvaluacionService.class);

    private final EvaluacionRepository evaluacionRepository;
    private final PreguntaRepository preguntaRepository;
    private final CursoService cursoService;
    private final AuditLogger auditLogger;
    private final CodigoGeneratorService codigoGeneratorService;
    private final NotificacionService notificacionService;
    private final com.lms.repository.InscripcionRepository inscripcionRepository;

    public EvaluacionService(EvaluacionRepository evaluacionRepository,
                             PreguntaRepository preguntaRepository,
                             CursoService cursoService,
                             AuditLogger auditLogger,
                             CodigoGeneratorService codigoGeneratorService,
                             NotificacionService notificacionService,
                             com.lms.repository.InscripcionRepository inscripcionRepository) {
        this.evaluacionRepository = evaluacionRepository;
        this.preguntaRepository = preguntaRepository;
        this.cursoService = cursoService;
        this.auditLogger = auditLogger;
        this.codigoGeneratorService = codigoGeneratorService;
        this.notificacionService = notificacionService;
        this.inscripcionRepository = inscripcionRepository;
    }

    /**
     * Crea una evaluacion con sus preguntas.
     */
    public Evaluacion crearEvaluacion(UUID cursoId, EvaluacionRequest request, UUID autorId) {
        Curso curso = cursoService.buscarPorId(cursoId)
                .orElseThrow(() -> new IllegalArgumentException("Curso no encontrado"));

        String hashInput = request.titulo().trim() + "|" +
                (request.descripcion() != null ? request.descripcion() : "") + "|" +
                (request.configuracion() != null ? request.configuracion() : "");

        Evaluacion evaluacion = Evaluacion.builder()
                .curso(curso)
                .titulo(request.titulo().trim())
                .descripcion(request.descripcion())
                .pesoTotal(request.pesoTotal() > 0 ? request.pesoTotal() : 100.0)
                .tiempoEstimadoMinutos(request.tiempoEstimadoMinutos())
                .configuracion(request.configuracion())
                .hashSha256(HashUtils.sha256(hashInput))
                .codigo(codigoGeneratorService.generarCodigo("EVA"))
                .notaMinima(request.notaMinima() != null ? request.notaMinima() : 60.0)
                .build();

        evaluacion = evaluacionRepository.save(evaluacion);

        // Crear preguntas si existen
        if (request.preguntas() != null && !request.preguntas().isEmpty()) {
            List<Pregunta> preguntas = new ArrayList<>();
            for (PreguntaRequest pReq : request.preguntas()) {
                Pregunta pregunta = Pregunta.builder()
                        .evaluacion(evaluacion)
                        .tipo(pReq.tipo())
                        .contenido(pReq.contenido())
                        .peso(pReq.peso() > 0 ? pReq.peso() : 1.0)
                        .tiempoRespuestaMs(pReq.tiempoRespuestaMs())
                        .opciones(pReq.opciones())
                        .respuestaCorrecta(pReq.respuestaCorrecta())
                        .hashSha256(HashUtils.sha256(pReq.contenido()))
                        .orden(pReq.orden())
                        .build();
                preguntas.add(pregunta);
            }
            preguntaRepository.saveAll(preguntas);
        }

        auditLogger.registrarEvento(
                "EVALUACION_CREADA",
                autorId,
                "CREAR",
                "EVALUACION",
                evaluacion.getId(),
                Map.of("cursoId", cursoId.toString(), "titulo", evaluacion.getTitulo(), "codigo", evaluacion.getCodigo())
        );

        try {
            List<com.lms.model.Inscripcion> inscritos = inscripcionRepository.findByCursoIdAndDeletedAtIsNull(cursoId);
            for (com.lms.model.Inscripcion insc : inscritos) {
                try {
                    notificacionService.enviarNotificacion(
                            insc.getUsuario().getId(),
                            "sistema",
                            "Nueva evaluación disponible",
                            "Se ha publicado la evaluación " + evaluacion.getTitulo()
                                    + " (" + evaluacion.getCodigo() + ") en el curso " + curso.getTitulo(),
                            "/cursos");
                } catch (Exception ex) {
                    log.error("Error al notificar evaluación {} al usuario {}", evaluacion.getId(), insc.getUsuario().getId(), ex);
                }
            }
        } catch (Exception e) {
            log.error("Error al notificar creación de evaluación {} a los inscritos del curso {}", evaluacion.getId(), cursoId, e);
        }

        return evaluacion;
    }

    /**
     * Actualiza una evaluación existente y reemplaza sus preguntas.
     */
    public Evaluacion actualizarEvaluacion(UUID evaluacionId, EvaluacionRequest request, UUID autorId) {
        Evaluacion evaluacion = evaluacionRepository.findById(evaluacionId)
                .orElseThrow(() -> new IllegalArgumentException("Evaluación no encontrada"));

        evaluacion.setTitulo(request.titulo().trim());
        evaluacion.setDescripcion(request.descripcion());
        evaluacion.setPesoTotal(request.pesoTotal() > 0 ? request.pesoTotal() : 100.0);
        evaluacion.setTiempoEstimadoMinutos(request.tiempoEstimadoMinutos());
        if (request.notaMinima() != null) {
            evaluacion.setNotaMinima(request.notaMinima());
        }
        evaluacion.setConfiguracion(request.configuracion());

        // Recalcular hash por integridad de datos
        String hashInput = request.titulo().trim() + "|" +
                (request.descripcion() != null ? request.descripcion() : "") + "|" +
                (request.configuracion() != null ? request.configuracion() : "");
        evaluacion.setHashSha256(HashUtils.sha256(hashInput));

        // Eliminar preguntas anteriores
        List<Pregunta> preguntasAnteriores = preguntaRepository.findByEvaluacionIdOrderByOrdenAsc(evaluacionId);
        if (!preguntasAnteriores.isEmpty()) {
            preguntaRepository.deleteAll(preguntasAnteriores);
        }

        // Crear nuevas preguntas
        if (request.preguntas() != null && !request.preguntas().isEmpty()) {
            List<Pregunta> preguntas = new ArrayList<>();
            for (PreguntaRequest pReq : request.preguntas()) {
                Pregunta pregunta = Pregunta.builder()
                        .evaluacion(evaluacion)
                        .tipo(pReq.tipo())
                        .contenido(pReq.contenido())
                        .peso(pReq.peso() > 0 ? pReq.peso() : 1.0)
                        .tiempoRespuestaMs(pReq.tiempoRespuestaMs())
                        .opciones(pReq.opciones())
                        .respuestaCorrecta(pReq.respuestaCorrecta())
                        .hashSha256(HashUtils.sha256(pReq.contenido()))
                        .orden(pReq.orden())
                        .build();
                preguntas.add(pregunta);
            }
            preguntaRepository.saveAll(preguntas);
        }

        evaluacion = evaluacionRepository.save(evaluacion);

        auditLogger.registrarEvento(
                "EVALUACION_ACTUALIZADA",
                autorId,
                "ACTUALIZAR",
                "EVALUACION",
                evaluacion.getId(),
                Map.of("titulo", evaluacion.getTitulo(), "codigo", evaluacion.getCodigo())
        );

        return evaluacion;
    }

    /**
     * Elimina (soft delete) una evaluación y sus preguntas.
     */
    public void eliminarEvaluacion(UUID evaluacionId, UUID autorId) {
        Evaluacion evaluacion = evaluacionRepository.findById(evaluacionId)
                .orElseThrow(() -> new IllegalArgumentException("Evaluación no encontrada"));

        // Soft delete de preguntas
        List<Pregunta> preguntas = preguntaRepository.findByEvaluacionIdOrderByOrdenAsc(evaluacionId);
        for (Pregunta p : preguntas) {
            p.setDeletedAt(LocalDateTime.now());
        }
        preguntaRepository.saveAll(preguntas);

        // Soft delete de la evaluación
        evaluacion.setDeletedAt(LocalDateTime.now());
        evaluacionRepository.save(evaluacion);

        auditLogger.registrarEvento(
                "EVALUACION_ELIMINADA",
                autorId,
                "ELIMINAR",
                "EVALUACION",
                evaluacion.getId(),
                Map.of("titulo", evaluacion.getTitulo(), "codigo", evaluacion.getCodigo())
        );
    }

    /**
     * Busca evaluación por ID.
     */
    @Transactional(readOnly = true)
    public Optional<Evaluacion> buscarPorId(UUID evaluacionId) {
        return evaluacionRepository.findById(evaluacionId);
    }

    /**
     * Lista evaluaciones de un curso.
     */
    @Transactional(readOnly = true)
    public List<Evaluacion> listarPorCurso(UUID cursoId) {
        System.out.println("[DEBUG EvaluacionService.listarPorCurso] cursoId=" + cursoId);
        List<Evaluacion> result = evaluacionRepository.findByCursoId(cursoId);
        System.out.println("[DEBUG EvaluacionService.listarPorCurso] result count="
                + (result != null ? result.size() : "null"));
        if (result != null && !result.isEmpty()) {
            System.out.println("[DEBUG EvaluacionService.listarPorCurso] first ev id="
                    + result.get(0).getId() + ", titulo=" + result.get(0).getTitulo());
        }
        return result;
    }

    /**
     * Obtiene evaluaciones paginadas de un curso.
     */
    @Transactional(readOnly = true)
    public Page<Evaluacion> listarPorCursoPaginado(UUID cursoId, Pageable pageable) {
        return evaluacionRepository.findByCursoId(cursoId, pageable);
    }

    /**
     * Busca evaluación por código alfanumérico.
     */
    @Transactional(readOnly = true)
    public Optional<Evaluacion> buscarPorCodigo(String codigo) {
        return evaluacionRepository.findByCodigo(codigo);
    }

    /**
     * Obtiene preguntas de una evaluacion.
     */
    @Transactional(readOnly = true)
    public List<Pregunta> obtenerPreguntas(UUID evaluacionId) {
        return preguntaRepository.findByEvaluacionIdOrderByOrdenAsc(evaluacionId);
    }
}
