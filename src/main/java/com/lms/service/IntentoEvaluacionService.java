package com.lms.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lms.model.*;
import com.lms.repository.*;
import com.lms.util.AuditLogger;
import com.lms.util.HashUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class IntentoEvaluacionService {

    private static final Logger log = LoggerFactory.getLogger(IntentoEvaluacionService.class);

    private final IntentoEvaluacionRepository intentoRepository;
    private final EvaluacionService evaluacionService;
    private final InscripcionRepository inscripcionRepository;
    private final CursoRepository cursoRepository;
    private final AuditLogger auditLogger;
    private final ObjectMapper mapper;
    private final UsuarioRepository usuarioRepository;

    public IntentoEvaluacionService(IntentoEvaluacionRepository intentoRepository,
                                     EvaluacionService evaluacionService,
                                     InscripcionRepository inscripcionRepository,
                                     CursoRepository cursoRepository,
                                     AuditLogger auditLogger,
                                     ObjectMapper mapper,
                                     UsuarioRepository usuarioRepository) {
        this.intentoRepository = intentoRepository;
        this.evaluacionService = evaluacionService;
        this.inscripcionRepository = inscripcionRepository;
        this.cursoRepository = cursoRepository;
        this.auditLogger = auditLogger;
        this.mapper = mapper;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional
    public IntentoEvaluacion iniciarIntento(UUID evaluacionId, UUID usuarioId) {
        Evaluacion evaluacion = evaluacionService.buscarPorId(evaluacionId)
                .orElseThrow(() -> new IllegalArgumentException("Evaluacion no encontrada: " + evaluacionId));

        // Verificar si ya existe un intento completado
        Optional<IntentoEvaluacion> completado = intentoRepository
                .findByEvaluacionIdAndUsuarioIdAndEstadoAndDeletedAtIsNull(
                        evaluacionId, usuarioId, "COMPLETADO");
        if (completado.isPresent()) {
            throw new IllegalStateException("Ya has completado esta evaluacion. Solicita un reinicio si necesitas rendirla nuevamente.");
        }

        // Buscar intento en curso previo (por si refresco la pagina)
        Optional<IntentoEvaluacion> enCurso = intentoRepository
                .findByEvaluacionIdAndUsuarioIdAndEstadoAndDeletedAtIsNull(
                        evaluacionId, usuarioId, "EN_CURSO");
        if (enCurso.isPresent()) {
            return enCurso.get();
        }

        // Obtener inscripcion
        Inscripcion inscripcion = inscripcionRepository
                .findByUsuarioIdAndCursoIdAndDeletedAtIsNull(usuarioId, evaluacion.getCurso().getId())
                .orElse(null);

        String hash = HashUtils.sha256(evaluacionId.toString() + "|" + usuarioId.toString() + "|" + LocalDateTime.now());

        IntentoEvaluacion intento = IntentoEvaluacion.builder()
                .evaluacion(evaluacion)
                .inscripcion(inscripcion)
                .usuario(null)
                .estado("EN_CURSO")
                .fechaInicio(LocalDateTime.now())
                .hashSha256(hash)
                .build();

        // Fix: usuario no es Usuario object, es relacion ManyToOne
        // Use Usuario reference
        intento.setUsuario(usuarioRepository.getReferenceById(usuarioId));

        intento = intentoRepository.save(intento);

        auditLogger.registrarEvento(
                "INTENTO_INICIADO", usuarioId, "CREAR", "INTENTO_EVALUACION",
                intento.getId(),
                Map.of("evaluacionId", evaluacionId.toString(), "codigo", evaluacion.getCodigo())
        );

        log.info("Intento {} iniciado para evaluacion {} usuario {}", intento.getId(), evaluacionId, usuarioId);
        return intento;
    }

    /**
     * Busca un intento EN_CURSO existente para la evaluacion y usuario dados.
     * Si no existe, crea uno nuevo (sin validacion de COMPLETADO).
     * Este metodo se usa desde el endpoint /responder para evitar la validacion
     * bloqueante de iniciarIntento() que rechaza si ya hay un COMPLETADO previo.
     */
    @Transactional
    public IntentoEvaluacion buscarOCrearIntentoEnCurso(UUID evaluacionId, UUID usuarioId) {
        // Buscar intento EN_CURSO
        java.util.Optional<IntentoEvaluacion> enCurso = intentoRepository
                .findByEvaluacionIdAndUsuarioIdAndEstadoAndDeletedAtIsNull(
                        evaluacionId, usuarioId, "EN_CURSO");
        if (enCurso.isPresent()) {
            return enCurso.get();
        }

        // Buscar intento COMPLETADO previo
        java.util.Optional<IntentoEvaluacion> completado = intentoRepository
                .findByEvaluacionIdAndUsuarioIdAndEstadoAndDeletedAtIsNull(
                        evaluacionId, usuarioId, "COMPLETADO");
        if (completado.isPresent()) {
            // Si ya hay un COMPLETADO, no se puede crear uno nuevo sin reinicio
            // Pero informamos con un mensaje claro
            throw new IllegalStateException(
                    "Ya has completado esta evaluacion. Solicita un reinicio a tu instructor.");
        }

        // No existe intento previo, crear uno nuevo como EN_CURSO
        Evaluacion evaluacion = evaluacionService.buscarPorId(evaluacionId)
                .orElseThrow(() -> new IllegalArgumentException("Evaluacion no encontrada: " + evaluacionId));

        Inscripcion inscripcion = inscripcionRepository
                .findByUsuarioIdAndCursoIdAndDeletedAtIsNull(usuarioId, evaluacion.getCurso().getId())
                .orElse(null);

        String hash = HashUtils.sha256(evaluacionId.toString() + "|" + usuarioId.toString() + "|" + LocalDateTime.now());

        IntentoEvaluacion intento = IntentoEvaluacion.builder()
                .evaluacion(evaluacion)
                .inscripcion(inscripcion)
                .usuario(usuarioRepository.getReferenceById(usuarioId))
                .estado("EN_CURSO")
                .fechaInicio(LocalDateTime.now())
                .hashSha256(hash)
                .build();

        intento = intentoRepository.save(intento);

        auditLogger.registrarEvento(
                "INTENTO_INICIADO", usuarioId, "CREAR", "INTENTO_EVALUACION",
                intento.getId(),
                Map.of("evaluacionId", evaluacionId.toString(), "codigo", evaluacion.getCodigo() != null ? evaluacion.getCodigo() : "N/A")
        );

        log.info("Intento {} creado para evaluacion {} usuario {}", intento.getId(), evaluacionId, usuarioId);
        return intento;
    }

    public java.util.Optional<IntentoEvaluacion> buscarIntentoCompletado(UUID evaluacionId, UUID usuarioId) {
        return intentoRepository.findByEvaluacionIdAndUsuarioIdAndEstadoAndDeletedAtIsNull(
                evaluacionId, usuarioId, "COMPLETADO");
    }

    @Transactional
    public IntentoEvaluacion entregarIntento(UUID intentoId, Map<String, Object> respuestasBody) {
        IntentoEvaluacion intento = intentoRepository.findById(intentoId)
                .orElseThrow(() -> new IllegalArgumentException("Intento no encontrado: " + intentoId));

        if (!"EN_CURSO".equals(intento.getEstado())) {
            throw new IllegalStateException("El intento ya fue entregado o esta reiniciado");
        }

        Evaluacion evaluacion = intento.getEvaluacion();
        List<Pregunta> preguntas = evaluacionService.obtenerPreguntas(evaluacion.getId());

        // Extraer respuestas del alumno (pueden venir directas o envueltas en "respuestas")
        Map<String, Object> respuestasAlumno = respuestasBody.containsKey("respuestas")
                ? (Map<String, Object>) respuestasBody.get("respuestas")
                : respuestasBody;

        // Respuestas se guardan como Map directamente (Hibernate maneja JSONB)

        // Corregir
        double sumaPesos = 0;
        double sumaAciertos = 0;

        java.util.List<Map<String, Object>> detallePreguntas = new java.util.ArrayList<>();

        for (Pregunta p : preguntas) {
            double peso = p.getPeso();
            sumaPesos += peso;

            // Buscar respuesta por UUID string (puede venir como String o Integer del JSON)
            String preguntaKey = p.getId().toString();
            Object respuestaObj = respuestasAlumno.get(preguntaKey);
            log.info("DEBUG Buscando clave {} encuentra valor {}", preguntaKey, respuestaObj);
            if (respuestaObj == null) {
                // Intentar con clave Integer si Jackson convirtio el string a numero
                for (Map.Entry<String, Object> entry : respuestasAlumno.entrySet()) {
                    if (entry.getKey().replace("-", "").equals(preguntaKey.replace("-", ""))
                            || entry.getKey().equals(preguntaKey)) {
                        respuestaObj = entry.getValue();
                        break;
                    }
                }
            }
            String respuestaAlumno = respuestaObj != null ? respuestaObj.toString() : "";

            boolean acierto = esCorrecta(p, respuestaAlumno);
            if (acierto) {
                sumaAciertos += peso;
            }

            // Guardar detalle por pregunta
            Map<String, Object> detalle = new java.util.HashMap<>();
            detalle.put("preguntaId", p.getId().toString());
            detalle.put("respuestaAlumno", respuestaAlumno);
            detalle.put("correcta", acierto);
            detalle.put("peso", p.getPeso());
            detallePreguntas.add(detalle);
        }

        double nota = sumaPesos > 0 ? (sumaAciertos / sumaPesos) * 100.0 : 0;
        boolean aprobado = nota >= evaluacion.getNotaMinima();

        Map<String, Object> resultadoFinal = new HashMap<>();
        resultadoFinal.put("detalle", detallePreguntas);
        intento.setRespuestas(resultadoFinal);
        intento.setNota(Math.round(nota * 100.0) / 100.0);
        intento.setAprobado(aprobado);
        intento.setEstado("COMPLETADO");
        intento.setFechaEntrega(LocalDateTime.now());
        intento.setHashSha256(HashUtils.sha256(
                intento.getId().toString() + "|" + nota + "|" + LocalDateTime.now()
        ));

        intento = intentoRepository.save(intento);

        // Marcar inscripcion como completada si aprobo
        if (aprobado && intento.getInscripcion() != null) {
            Inscripcion insc = intento.getInscripcion();
            if (!insc.isCompletado() || insc.getNota() == null) {
                insc.setCompletado(true);
                insc.setNota(nota);
                insc.setFechaCompletado(LocalDateTime.now());
                inscripcionRepository.save(insc);
            }
        }

        auditLogger.registrarEvento(
                aprobado ? "INTENTO_APROBADO" : "INTENTO_COMPLETADO",
                intento.getUsuario().getId(), "ACTUALIZAR", "INTENTO_EVALUACION",
                intento.getId(),
                Map.of("nota", String.valueOf(nota), "aprobado", String.valueOf(aprobado),
                       "evaluacionCodigo", evaluacion.getCodigo())
        );

        log.info("Intento {} entregado. Nota: {}, Aprobado: {}", intento.getId(), nota, aprobado);
        return intento;
    }

    @Transactional
    public IntentoEvaluacion reiniciarIntento(UUID evaluacionId, UUID usuarioId,
                                               UUID adminUserId, String motivo) {
        // Marcar intentos anteriores como REINICIADO
        List<IntentoEvaluacion> anteriores = intentoRepository
                .findIntentosByEvaluacionAndUsuario(evaluacionId, usuarioId);
        for (IntentoEvaluacion ia : anteriores) {
            if (!"REINICIADO".equals(ia.getEstado())) {
                ia.setEstado("REINICIADO");
                ia.setReiniciadoPor(adminUserId);
                ia.setMotivoReinicio(motivo);
                intentoRepository.save(ia);
            }
        }

        // Crear nuevo intento
        return iniciarIntento(evaluacionId, usuarioId);
    }

    private boolean esCorrecta(Pregunta pregunta, String respuestaAlumno) {
        if (respuestaAlumno == null || respuestaAlumno.isEmpty()) {
            return false;
        }

        String tipo = pregunta.getTipo();

        // Para opcion_unica, opcion_multiple y verdadero_falso,
        // la respuesta correcta esta en el JSON de opciones (campo "correcta" booleano)
        String opcionesJson = pregunta.getOpciones();
        if (opcionesJson != null && !opcionesJson.isEmpty()
                && ("opcion_unica".equals(tipo) || "OPCION_UNICA".equals(tipo)
                || "opcion_multiple".equals(tipo) || "OPCION_MULTIPLE".equals(tipo)
                || "verdadero_falso".equals(tipo) || "VERDADERO_FALSO".equals(tipo) || "VF".equals(tipo))) {
            try {
                List<Map<String, Object>> opcionesLista = mapper.readValue(opcionesJson,
                        new TypeReference<List<Map<String, Object>>>() {});
                // Encontrar indices de opciones correctas
                Set<String> indicesCorrectos = new HashSet<>();
                for (int i = 0; i < opcionesLista.size(); i++) {
                    Map<String, Object> op = opcionesLista.get(i);
                    if (op.containsKey("correcta") && Boolean.TRUE.equals(op.get("correcta"))) {
                        indicesCorrectos.add(String.valueOf(op.get("texto")));
                    }
                }

                if ("opcion_unica".equals(tipo) || "OPCION_UNICA".equals(tipo)
                        || "verdadero_falso".equals(tipo) || "VERDADERO_FALSO".equals(tipo) || "VF".equals(tipo)) {
                    // Un solo texto esperado
                    return indicesCorrectos.contains(respuestaAlumno.trim());
                } else {
                    // Multiple: comparar conjuntos de textos (separados por coma)
                    Set<String> setAlumno = new HashSet<>();
                    for (String s : respuestaAlumno.split(",")) {
                        setAlumno.add(s.trim());
                    }
                    return setAlumno.equals(indicesCorrectos);
                }
            } catch (Exception e) {
                log.warn("Error al parsear opciones para correccion: {}", e.getMessage());
                return false;
            }
        }

        // Fallback: usar respuestaCorrecta del campo directo (para respuesta_corta, desarrollo)
        String correcta = pregunta.getRespuestaCorrecta();
        if (correcta == null || correcta.isEmpty()) {
            return false;
        }
        return respuestaAlumno.trim().equalsIgnoreCase(correcta.trim());
    }
}
