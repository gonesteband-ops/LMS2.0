package com.lms.service;

import com.lms.dto.CursoRequest;
import com.lms.model.Curso;
import com.lms.model.VersionCurso;
import com.lms.repository.CursoRepository;
import com.lms.repository.InscripcionRepository;
import com.lms.repository.VersionCursoRepository;
import com.lms.util.AuditLogger;
import com.lms.util.HashUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Servicio de gestión de cursos con versionado completo.
 * <p>
 * Cada modificación crea una nueva versión en versione s_curso.
 */
@Service
@Transactional
public class CursoService {

    private static final Logger log = LoggerFactory.getLogger(CursoService.class);

    private final CursoRepository cursoRepository;
    private final VersionCursoRepository versionCursoRepository;
    private final InscripcionRepository inscripcionRepository;
    private final AuditLogger auditLogger;

    public CursoService(CursoRepository cursoRepository,
                        VersionCursoRepository versionCursoRepository,
                        InscripcionRepository inscripcionRepository,
                        AuditLogger auditLogger) {
        this.cursoRepository = cursoRepository;
        this.versionCursoRepository = versionCursoRepository;
        this.inscripcionRepository = inscripcionRepository;
        this.auditLogger = auditLogger;
    }

    /**
     * Crea un nuevo curso con su versión inicial (v1).
     */
    public Curso crearCurso(CursoRequest request, UUID autorId) {
        String hashSha256 = HashUtils.sha256(request.titulo() + "|" + request.descripcion());

        LocalDateTime fechaLimite = parseFechaLimite(request.fechaLimite());

        Curso curso = Curso.builder()
                .titulo(request.titulo().trim())
                .descripcion(request.descripcion() != null ? request.descripcion().trim() : null)
                .versionActual(1)
                .estado(request.estado() != null ? request.estado() : "borrador")
                .categoria(request.categoria())
                .fechaLimite(fechaLimite)
                .requiereFirmaCumplimiento(request.requiereFirmaCumplimiento())
                .duracionEstimada(request.duracionEstimada())
                .hashSha256(hashSha256)
                .build();

        curso = cursoRepository.save(curso);

        // Crear versión inicial
        VersionCurso version = VersionCurso.builder()
                .curso(curso)
                .numeroVersion(1)
                .contenido(request.contenido())
                .hashContenido(HashUtils.sha256(request.contenido() != null ? request.contenido() : ""))
                .hashVersionAnterior(null) // Versión génesis
                .autorId(autorId)
                .motivoCambio("Versión inicial")
                .hashSha256(HashUtils.sha256("1|" + request.contenido()))
                .build();

        versionCursoRepository.save(version);

        auditLogger.registrarEvento(
                "CURSO_CREADO",
                autorId,
                "CREAR",
                "CURSO",
                curso.getId(),
                Map.of("titulo", curso.getTitulo(), "version", 1)
        );

        return curso;
    }

    /**
     * Actualiza un curso, creando una nueva versión.
     */
    public Curso actualizarCurso(UUID cursoId, CursoRequest request, UUID autorId) {
        Curso curso = cursoRepository.findById(cursoId)
                .orElseThrow(() -> new IllegalArgumentException("Curso no encontrado"));

        int nuevaVersion = curso.getVersionActual() + 1;

        // Obtener hash de la versión anterior
        Optional<VersionCurso> ultimaVersion = versionCursoRepository
                .findTopByCursoIdOrderByNumeroVersionDesc(cursoId);
        String hashAnterior = ultimaVersion.map(VersionCurso::getHashSha256).orElse(null);

        // Actualizar curso
        curso.setTitulo(request.titulo().trim());
        curso.setDescripcion(request.descripcion() != null ? request.descripcion().trim() : null);
        curso.setVersionActual(nuevaVersion);
        curso.setEstado(request.estado());
        curso.setCategoria(request.categoria());
        curso.setFechaLimite(parseFechaLimite(request.fechaLimite()));
        curso.setRequiereFirmaCumplimiento(request.requiereFirmaCumplimiento());
        curso.setDuracionEstimada(request.duracionEstimada());
        curso.setHashSha256(HashUtils.sha256(request.titulo() + "|" + nuevaVersion));
        curso = cursoRepository.save(curso);

        // Crear nueva versión
        String contenido = request.contenido() != null ? request.contenido() : "";
        VersionCurso version = VersionCurso.builder()
                .curso(curso)
                .numeroVersion(nuevaVersion)
                .contenido(contenido)
                .hashContenido(HashUtils.sha256(contenido))
                .hashVersionAnterior(hashAnterior)
                .autorId(autorId)
                .motivoCambio(request.motivoCambio() != null ? request.motivoCambio() : "Actualización")
                .hashSha256(HashUtils.sha256Encadenado(nuevaVersion + "|" + contenido, hashAnterior))
                .build();

        versionCursoRepository.save(version);

        auditLogger.registrarEvento(
                "CURSO_ACTUALIZADO",
                autorId,
                "ACTUALIZAR",
                "CURSO",
                curso.getId(),
                Map.of("titulo", curso.getTitulo(), "nuevaVersion", nuevaVersion, "hashAnterior", hashAnterior)
        );

        return curso;
    }

    /**
     * Lista cursos activos con paginación.
     */
    @Transactional(readOnly = true)
    public Page<Curso> listarCursos(Pageable pageable) {
        return cursoRepository.findByDeletedAtIsNull(pageable);
    }

    /**
     * Lista cursos según el rol del usuario:
     * - ESTUDIANTE: solo los cursos en los que está inscrito
     * - ADMIN/INSTRUCTOR/SUPERADMIN: todos los cursos activos
     */
    @Transactional(readOnly = true)
    public Page<Curso> listarCursosSegunRol(UUID usuarioId, Collection<? extends GrantedAuthority> authorities, Pageable pageable) {
        boolean isAdminOrInstructor = authorities.stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_SUPERADMIN") ||
                              a.getAuthority().equals("ROLE_INSTRUCTOR") ||
                              a.getAuthority().equals("ROLE_SUPERADMIN"));

        if (isAdminOrInstructor) {
            return listarCursos(pageable);
        } else {
            // Estudiante: solo cursos inscritos
            List<Curso> cursosInscritos = inscripcionRepository.findCursosInscritosByUsuarioId(usuarioId);
            // Convertir a Page
            int start = (int) pageable.getOffset();
            int end = Math.min((start + pageable.getPageSize()), cursosInscritos.size());
            List<Curso> pageContent = start > cursosInscritos.size() ? List.of() : cursosInscritos.subList(start, end);
            return new PageImpl<>(pageContent, pageable, cursosInscritos.size());
        }
    }

    /**
     * Busca curso por ID.
     */
    @Transactional(readOnly = true)
    public Optional<Curso> buscarPorId(UUID id) {
        return cursoRepository.findByIdAndDeletedAtIsNull(id);
    }

    /**
     * Lista los cursos activos en los que un usuario está inscrito.
     */
    @Transactional(readOnly = true)
    public List<Curso> listarCursosPorUsuario(UUID usuarioId) {
        return inscripcionRepository.findByUsuarioIdAndDeletedAtIsNull(usuarioId)
                .stream()
                .map(i -> i.getCurso())
                .collect(Collectors.toList());
    }

    /**
     * Obtiene el historial de versiones de un curso.
     */
    @Transactional(readOnly = true)
    public Page<VersionCurso> obtenerVersiones(UUID cursoId, Pageable pageable) {
        return versionCursoRepository.findByCursoIdOrderByNumeroVersionDesc(cursoId, pageable);
    }

    /**
     * Soft-delete de un curso.
     */
    public void eliminarCurso(UUID id, UUID eliminadoPor, String motivo) {
        cursoRepository.findById(id).ifPresent(curso -> {
            curso.setDeletedAt(LocalDateTime.now());
            curso.setDeletedBy(eliminadoPor);
            curso.setReason(motivo);
            cursoRepository.save(curso);

            auditLogger.registrarEvento(
                    "CURSO_ELIMINADO",
                    eliminadoPor,
                    "SOFT_DELETE",
                    "CURSO",
                    id,
                    Map.of("motivo", motivo)
            );
        });
    }

    /**
     * Parsea una fecha límite desde un string ISO (yyyy-MM-dd o yyyy-MM-dd'T'HH:mm:ss).
     * Retorna null si el string es null o vacío.
     */
    private LocalDateTime parseFechaLimite(String fechaStr) {
        if (fechaStr == null || fechaStr.isBlank()) {
            return null;
        }
        try {
            // Intentar formato con fecha y hora (ISO)
            return LocalDateTime.parse(fechaStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (DateTimeParseException e) {
            try {
                // Intentar solo fecha (media noche)
                LocalDate date = LocalDate.parse(fechaStr, DateTimeFormatter.ISO_LOCAL_DATE);
                return date.atTime(LocalTime.MAX);
            } catch (DateTimeParseException e2) {
                log.warn("Formato de fecha límite inválido: {}", fechaStr);
                return null;
            }
        }
    }

}
