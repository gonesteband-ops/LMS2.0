package com.lms.service;

import com.lms.dto.InscripcionMasivaReport;
import com.lms.dto.InscripcionResponse;
import com.lms.model.Curso;
import com.lms.model.Inscripcion;
import com.lms.model.Usuario;
import com.lms.repository.CursoRepository;
import com.lms.repository.InscripcionRepository;
import com.lms.repository.UsuarioRepository;
import com.lms.util.AuditLogger;
import com.lms.util.HashUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Servicio de gestión de inscripciones (participación de usuarios en cursos).
 * <p>
 * Soporta:
 * <ul>
 *   <li>Inscripción manual 1-to-1</li>
 *   <li>Inscripción masiva via XLSX/CSV</li>
 *   <li>Consulta de cursos del usuario (pendientes/completados)</li>
 *   <li>Marcar curso como completado con nota</li>
 * </ul>
 */
@Service
@Transactional
public class InscripcionService {

    private static final Logger log = LoggerFactory.getLogger(InscripcionService.class);

    private final InscripcionRepository inscripcionRepository;
    private final CursoRepository cursoRepository;
    private final UsuarioRepository usuarioRepository;
    private final AuditLogger auditLogger;
    private final NotificacionService notificacionService;

    public InscripcionService(InscripcionRepository inscripcionRepository,
                              CursoRepository cursoRepository,
                              UsuarioRepository usuarioRepository,
                              AuditLogger auditLogger,
                              NotificacionService notificacionService) {
        this.inscripcionRepository = inscripcionRepository;
        this.cursoRepository = cursoRepository;
        this.usuarioRepository = usuarioRepository;
        this.auditLogger = auditLogger;
        this.notificacionService = notificacionService;
    }

    /**
     * Resuelve un identificador de usuario (UUID, RUT o email) a un UUID.
     * <p>
     * El orden de resolución es:
     * <ol>
     *   <li>Intenta parsear como UUID directamente</li>
     *   <li>Busca por RUT (con o sin formato de puntos)</li>
     *   <li>Busca por email</li>
     * </ol>
     *
     * @param identificador UUID, RUT o email del usuario
     * @return UUID del usuario encontrado
     * @throws IllegalArgumentException si no se encuentra el usuario
     */
    public UUID resolverIdentificador(String identificador) {
        // 1. Intentar como UUID
        try {
            return UUID.fromString(identificador);
        } catch (IllegalArgumentException ignored) {}

        // 2. Intentar como RUT — buscar con y sin formato de puntos
        //    (la BD almacena con puntos, ej: "16.478.280-8")
        Optional<Usuario> byRut = usuarioRepository.findByRut(identificador);
        if (byRut.isPresent()) {
            return byRut.get().getId();
        }
        // Si no tiene puntos, formatear y buscar de nuevo
        String sinPuntos = identificador.replaceAll("[.\\s]", "");
        if (!sinPuntos.equals(identificador)) {
            // Ya contiene puntos, ya se buscó arriba
        } else {
            String rutFormateado = formatearRut(identificador);
            if (!rutFormateado.equals(identificador)) {
                byRut = usuarioRepository.findByRut(rutFormateado);
                if (byRut.isPresent()) {
                    return byRut.get().getId();
                }
            }
        }

        // 3. Intentar como email
        Optional<Usuario> byEmail = usuarioRepository.findByEmailAndDeletedAtIsNull(identificador);
        if (byEmail.isPresent()) {
            return byEmail.get().getId();
        }

        throw new IllegalArgumentException("Usuario no encontrado: " + identificador);
    }

    /**
     * Formatea un RUT sin puntos al formato estándar XX.XXX.XXX-Y.
     * <p>
     * Ejemplo: "16478280-8" → "16.478.280-8"
     *
     * @param rut RUT sin formato
     * @return RUT formateado con puntos
     */
    private String formatearRut(String rut) {
        if (rut == null || rut.isBlank()) return rut;
        // Limpiar: solo dígitos y K
        String limpio = rut.replaceAll("[^\\dkK]", "").toUpperCase();
        if (limpio.length() < 2) return rut;
        String cuerpo = limpio.substring(0, limpio.length() - 1);
        String dv = limpio.substring(limpio.length() - 1);

        // Insertar puntos en el cuerpo cada 3 dígitos desde la derecha
        StringBuilder sb = new StringBuilder();
        int len = cuerpo.length();
        for (int i = 0; i < len; i++) {
            if (i > 0 && (len - i) % 3 == 0) {
                sb.append('.');
            }
            sb.append(cuerpo.charAt(i));
        }
        sb.append('-').append(dv);
        return sb.toString();
    }

    /**
     * Inscribe un usuario en un curso (1-to-1).
     *
     * @param cursoId  ID del curso
     * @param usuarioId ID del usuario a inscribir
     * @return InscripcionResponse con los datos de la inscripción
     */
    public InscripcionResponse inscribirUsuario(UUID cursoId, UUID usuarioId) {
        return inscribirUsuarioConFirma(cursoId, usuarioId, false, null, null);
    }

    /**
     * Inscribe un usuario en un curso con validación de firma digital si es requerida.
     *
     * @param cursoId                   ID del curso
     * @param usuarioId                 ID del usuario a inscribir
     * @param declaracionJuradaAceptada Indica si el usuario aceptó la declaración jurada
     * @param ipCliente                 IP del cliente (para trazabilidad)
     * @param timestamp                 Timestamp de la firma
     * @return InscripcionResponse con los datos de la inscripción
     */
    public InscripcionResponse inscribirUsuarioConFirma(UUID cursoId, UUID usuarioId,
                                                        boolean declaracionJuradaAceptada,
                                                        String ipCliente, LocalDateTime timestamp) {
        Curso curso = cursoRepository.findByIdAndDeletedAtIsNull(cursoId)
                .orElseThrow(() -> new IllegalArgumentException("Curso no encontrado: " + cursoId));

        String estado = curso.getEstado();
        if (estado == null || !"publicado".equalsIgnoreCase(estado)) {
            throw new IllegalArgumentException(
                "No se puede inscribir: el curso está en estado " +
                (estado != null ? estado.toUpperCase() : "SIN ESTADO") +
                ". Solo cursos PUBLICADOS aceptan inscripciones."
            );
        }

        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + usuarioId));

        // Verificar si ya está inscrito
        if (inscripcionRepository.existsByUsuarioIdAndCursoIdAndDeletedAtIsNull(usuarioId, cursoId)) {
            throw new IllegalStateException("El usuario ya está inscrito en este curso");
        }

        // Validar firma digital si el curso la requiere
        String firmaHash = null;
        LocalDateTime timestampFirma = null;
        String ipFirma = null;
        
        if (curso.isRequiereFirmaCumplimiento()) {
            if (!declaracionJuradaAceptada) {
                throw new IllegalArgumentException("Se requiere aceptar la declaración jurada para inscribirse en este curso");
            }
            
            // Capturar datos de firma
            timestampFirma = timestamp != null ? timestamp : LocalDateTime.now();
            ipFirma = ipCliente;
            firmaHash = HashUtils.sha256(cursoId.toString() + "|" + usuarioId.toString() + "|" + timestampFirma.toString());
        } else {
            // Si no requiere firma, mantener valores por defecto
            declaracionJuradaAceptada = false;
        }

        String hashSha256 = HashUtils.sha256(cursoId.toString() + "|" + usuarioId.toString() + "|" + LocalDateTime.now());

        Inscripcion inscripcion = Inscripcion.builder()
                .curso(curso)
                .usuario(usuario)
                .fechaInscripcion(LocalDateTime.now())
                .fechaLimite(curso.getFechaLimite())
                .completado(false)
                .declaracionJuradaAceptada(declaracionJuradaAceptada)
                .ipFirma(ipFirma)
                .timestampFirma(timestampFirma)
                .firmaHash(firmaHash)
                .hashSha256(hashSha256)
                .build();

        inscripcion = inscripcionRepository.save(inscripcion);

        try {
            notificacionService.enviarNotificacion(
                    usuarioId,
                    "sistema",
                    "Inscripción a curso",
                    "Has sido inscrito al curso " + curso.getTitulo(),
                    "/cursos");
        } catch (Exception e) {
            log.error("Error al notificar inscripción del usuario {} en el curso {}", usuarioId, cursoId, e);
        }

        auditLogger.registrarEvento(
                "INSCRIPCION_CREADA",
                usuarioId,
                "CREAR",
                "INSCRIPCION",
                inscripcion.getId(),
                Map.of("cursoId", cursoId.toString(), "usuarioId", usuarioId.toString())
        );

        log.info("Usuario {} inscrito en curso {}", usuarioId, cursoId);
        return InscripcionResponse.fromEntity(inscripcion);
    }

    /**
     * Inscripción masiva desde archivo CSV/XLSX.
     * <p>
     * Formato esperado (CSV): email o UUID por línea.
     * Retorna un reporte detallado con el desglose por escenario:
     * exitosos, no encontrados, ya inscritos y errores.
     *
     * @param cursoId ID del curso
     * @param file    Archivo con lista de usuarios (email o UUID)
     * @return InscripcionMasivaReport con el desglose completo
     */
    public InscripcionMasivaReport inscribirMasivo(UUID cursoId, MultipartFile file) {
        Curso curso = cursoRepository.findByIdAndDeletedAtIsNull(cursoId)
                .orElseThrow(() -> new IllegalArgumentException("Curso no encontrado: " + cursoId));

        List<InscripcionResponse> exitosos = new ArrayList<>();
        List<String> noEncontrados = new ArrayList<>();
        List<String> yaInscritos = new ArrayList<>();
        List<String> errores = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            List<String> lineas = reader.lines()
                    .map(String::trim)
                    .filter(l -> !l.isEmpty() && !l.startsWith("#") && !l.startsWith("email") && !l.startsWith("usuario"))
                    .collect(Collectors.toList());

            for (String linea : lineas) {
                try {
                    UUID usuarioId = resolverIdentificador(linea);
                    InscripcionResponse response = inscribirUsuario(cursoId, usuarioId);
                    exitosos.add(response);
                } catch (IllegalStateException e) {
                    // Escenario: ya está inscrito
                    log.warn("Ya inscrito '{}' en curso {}: {}", linea, cursoId, e.getMessage());
                    yaInscritos.add(linea);
                } catch (IllegalArgumentException e) {
                    // Escenario: usuario no encontrado (email inválido, UUID no existe, etc.)
                    log.warn("No encontrado '{}' en curso {}: {}", linea, cursoId, e.getMessage());
                    noEncontrados.add(linea);
                } catch (Exception e) {
                    // Escenario: error inesperado (formato de línea inválido, etc.)
                    log.warn("Error inscribiendo usuario '{}' en curso {}: {}", linea, cursoId, e.getMessage());
                    errores.add(linea);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error procesando archivo de inscripción masiva", e);
        }

        int totalProcesados = exitosos.size() + noEncontrados.size() + yaInscritos.size() + errores.size();

        auditLogger.registrarEvento(
                "INSCRIPCION_MASIVA",
                null,
                "CREAR",
                "INSCRIPCION",
                cursoId,
                Map.of(
                        "total", totalProcesados,
                        "exitosos", exitosos.size(),
                        "noEncontrados", noEncontrados.size(),
                        "yaInscritos", yaInscritos.size(),
                        "errores", errores.size()
                )
        );

        return new InscripcionMasivaReport(
                totalProcesados,
                exitosos.size(),
                noEncontrados.size(),
                yaInscritos.size(),
                errores.size(),
                exitosos,
                noEncontrados,
                yaInscritos,
                errores
        );
    }

    /**
     * Verifica si un usuario está inscrito en un curso activo.
     *
     * @param usuarioId ID del usuario
     * @param cursoId   ID del curso
     * @return true si el usuario está inscrito y la inscripción no fue eliminada
     */
    @Transactional(readOnly = true)
    public boolean isUsuarioInscritoEnCurso(UUID usuarioId, UUID cursoId) {
        return inscripcionRepository.existsByUsuarioIdAndCursoIdAndDeletedAtIsNull(usuarioId, cursoId);
    }

    /**
     * Lista los cursos en los que un usuario está inscrito (activos).
     */
    @Transactional(readOnly = true)
    public List<InscripcionResponse> listarMisCursos(UUID usuarioId) {
        List<Inscripcion> inscripciones = inscripcionRepository
                .findByUsuarioIdAndDeletedAtIsNull(usuarioId);
        return inscripciones.stream()
                .map(InscripcionResponse::fromEntity)
                .sorted((a, b) -> {
                    int pa = prioridadEstado(a.cursoEstado());
                    int pb = prioridadEstado(b.cursoEstado());
                    return Integer.compare(pa, pb);
                })
                .collect(Collectors.toList());
    }

    private int prioridadEstado(String estado) {
        if ("publicado".equalsIgnoreCase(estado)) return 0;
        if ("borrador".equalsIgnoreCase(estado)) return 1;
        return 2;
    }

    /**
     * Lista los cursos pendientes (no completados) del usuario.
     */
    @Transactional(readOnly = true)
    public List<InscripcionResponse> listarPendientes(UUID usuarioId) {
        return inscripcionRepository.findPendientesByUsuarioId(usuarioId)
                .stream()
                .map(InscripcionResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Lista los cursos completados del usuario.
     */
    @Transactional(readOnly = true)
    public List<InscripcionResponse> listarCompletados(UUID usuarioId) {
        return inscripcionRepository.findCompletadosByUsuarioId(usuarioId)
                .stream()
                .map(InscripcionResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Lista los participantes (inscripciones) de un curso específico.
     *
     * @param cursoId ID del curso
     * @return Lista de InscripcionResponse con los participantes
     */
    @Transactional(readOnly = true)
    public List<InscripcionResponse> listarParticipantesPorCurso(UUID cursoId) {
        return inscripcionRepository.findByCursoIdAndDeletedAtIsNull(cursoId)
                .stream()
                .map(InscripcionResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Marca un curso como completado para un usuario.
     *
     * @param cursoId   ID del curso
     * @param usuarioId ID del usuario
     * @param nota      Nota obtenida (opcional)
     * @return InscripcionResponse actualizada
     */
    public InscripcionResponse completarCurso(UUID cursoId, UUID usuarioId, Double nota) {
        Inscripcion inscripcion = inscripcionRepository
                .findByUsuarioIdAndCursoIdAndDeletedAtIsNull(usuarioId, cursoId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Inscripción no encontrada para usuario " + usuarioId + " en curso " + cursoId));

        inscripcion.setCompletado(true);
        inscripcion.setNota(nota);
        inscripcion.setFechaCompletado(LocalDateTime.now());

        // Recalcular hash
        String hashSha256 = HashUtils.sha256(
            inscripcion.getId().toString() + "|" +
            cursoId.toString() + "|" +
            usuarioId.toString() + "|" +
            "COMPLETADO" + "|" +
            (nota != null ? nota.toString() : "null") + "|" +
            LocalDateTime.now()
        );
        inscripcion.setHashSha256(hashSha256);

        inscripcion = inscripcionRepository.save(inscripcion);

        auditLogger.registrarEvento(
                "CURSO_COMPLETADO",
                usuarioId,
                "ACTUALIZAR",
                "INSCRIPCION",
                inscripcion.getId(),
                Map.of("cursoId", cursoId.toString(), "nota", nota != null ? nota.toString() : "N/A")
        );

        return InscripcionResponse.fromEntity(inscripcion);
    }

    /**
     * Obtiene el conteo de cursos pendientes y completados para el dashboard.
     */
    @Transactional(readOnly = true)
    public Map<String, Long> obtenerEstadisticas(UUID usuarioId) {
        long pendientes = inscripcionRepository.countByUsuarioIdAndCompletadoFalseAndDeletedAtIsNull(usuarioId);
        long completados = inscripcionRepository.countByUsuarioIdAndCompletadoTrueAndDeletedAtIsNull(usuarioId);
        return Map.of("pendientes", pendientes, "completados", completados);
    }

    /**
     * Elimina (soft delete) una inscripción.
     */
    public void eliminarInscripcion(UUID inscripcionId, UUID eliminadoPor, String motivo) {
        Inscripcion inscripcion = inscripcionRepository.findById(inscripcionId)
                .orElseThrow(() -> new IllegalArgumentException("Inscripción no encontrada: " + inscripcionId));

        inscripcion.setDeletedAt(LocalDateTime.now());
        inscripcion.setDeletedBy(eliminadoPor);
        inscripcion.setReason(motivo);
        inscripcionRepository.save(inscripcion);

        auditLogger.registrarEvento(
                "INSCRIPCION_ELIMINADA",
                eliminadoPor,
                "ELIMINAR",
                "INSCRIPCION",
                inscripcionId,
                Map.of("motivo", motivo)
        );
    }

    /**
     * Marca una inscripción como completada de forma forzosa por un supervisor.
     * 
     * @param inscripcionId ID de la inscripción a forzar
     * @param adminUserId ID del administrador que fuerza la completación
     * @param motivo Motivo de la forzada (obligatorio)
     * @param comentario Comentario adicional (opcional)
     * @return InscripcionResponse actualizada
     */
    @Transactional
    public InscripcionResponse forzarCompletado(UUID inscripcionId, UUID adminUserId, 
                                               String motivo, String comentario) {
        Inscripcion inscripcion = inscripcionRepository.findById(inscripcionId)
                .orElseThrow(() -> new IllegalArgumentException("Inscripción no encontrada: " + inscripcionId));

        // Validar que no esté ya completada
        if (inscripcion.isCompletado()) {
            throw new IllegalStateException("La inscripción ya está completada");
        }

        // Establecer campos de forzado
        inscripcion.setCompletado(true);
        inscripcion.setFechaCompletado(LocalDateTime.now());
        inscripcion.setForzadoPorSupervisor(true);
        inscripcion.setMotivoForzado(motivo);
        inscripcion.setComentarioForzado(comentario);
        inscripcion.setForzadoPorUsuarioId(adminUserId);
        inscripcion.setForzadoEn(LocalDateTime.now());
        inscripcion.setEtiquetaForzado("APROBADO_POR_SUPERVISOR_FALLA_SCORM");
        
        // Asignar nota mínima si es null
        if (inscripcion.getNota() == null) {
            inscripcion.setNota(4.0);
        }

        // Recalcular hash con los nuevos datos
        String hashSha256 = HashUtils.sha256(
            inscripcion.getId().toString() + "|" +
            inscripcion.getCurso().getId().toString() + "|" +
            inscripcion.getUsuario().getId().toString() + "|" +
            "COMPLETADO_FORZADO" + "|" +
            inscripcion.getNota().toString() + "|" +
            LocalDateTime.now()
        );
        inscripcion.setHashSha256(hashSha256);

        inscripcion = inscripcionRepository.save(inscripcion);

        auditLogger.registrarEvento(
                "INSCRIPCION_COMPLETADA_FORZADA",
                adminUserId,
                "ACTUALIZAR",
                "INSCRIPCION",
                inscripcionId,
                Map.of(
                    "motivo", motivo,
                    "comentario", comentario != null ? comentario : "N/A",
                    "etiqueta", "APROBADO_POR_SUPERVISOR_FALLA_SCORM"
                )
        );

        log.info("Inscripción {} forzada como completada por administrador {}", inscripcionId, adminUserId);
        return InscripcionResponse.fromEntity(inscripcion);
    }
}
