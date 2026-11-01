package com.lms.service;

import com.lms.model.Curso;
import com.lms.model.Notificacion;
import com.lms.model.VersionCurso;
import com.lms.repository.CursoRepository;
import com.lms.repository.VersionCursoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Tareas programadas del sistema de notificaciones automáticas.
 * <p>
 * Ejecuta verificaciones periódicas de vencimientos de cursos,
 * limpieza de notificaciones antiguas y notificaciones de sistema.
 * <p>
 * Configuración de cron (por defecto):
 * <ul>
 *   <li><strong>verificarVencimientos</strong>: 0 0 2 * * ? (2:00 AM, diario)</li>
 *   <li><strong>limpiarNotificacionesAntiguas</strong>: 0 0 3 * * ? (3:00 AM, diario)</li>
 *   <li><strong>notificarResumenSemanal</strong>: 0 0 8 * * MON (8:00 AM, cada lunes)</li>
 * </ul>
 */
@Service
public class NotificacionScheduler {

    private static final Logger log = LoggerFactory.getLogger(NotificacionScheduler.class);

    private final CursoRepository cursoRepository;
    private final VersionCursoRepository versionCursoRepository;
    private final NotificacionService notificacionService;

    /**
     * Umbral en meses para considerar un curso como "sin actualizar" (default: 6).
     */
    @Value("${app.notificaciones.umbral-meses-sin-actualizar:6}")
    private int umbralMesesSinActualizar;

    public NotificacionScheduler(CursoRepository cursoRepository,
                                 VersionCursoRepository versionCursoRepository,
                                 NotificacionService notificacionService) {
        this.cursoRepository = cursoRepository;
        this.versionCursoRepository = versionCursoRepository;
        this.notificacionService = notificacionService;
    }

    /**
     * Verifica diariamente (2:00 AM) los cursos publicados y notifica
     * sobre aquellos que se acercan a su posible vencimiento.
     * <p>
     * Actualmente utiliza {@code updatedAt} como heurística. Cuando se
     * agregue el campo {@code fecha_vencimiento} a la entidad Curso,
     * esta tarea deberá actualizarse para usar ese campo directamente.
     */
    @Scheduled(cron = "${app.notificaciones.cron-vencimientos:0 0 2 * * ?}")
    @Transactional
    public void verificarVencimientos() {
        log.info("[SCHEDULER] Iniciando verificación de vencimientos de cursos...");

        LocalDateTime ahora = LocalDateTime.now();
        LocalDateTime umbral = ahora.minusMonths(umbralMesesSinActualizar);

        // Obtener todos los cursos activos (no eliminados)
        List<Curso> cursosActivos = cursoRepository.findByDeletedAtIsNull(Pageable.unpaged()).getContent();

        // Filtrar cursos publicados sin actualizar en > N meses
        List<Curso> cursosSinActualizar = cursosActivos.stream()
                .filter(c -> "publicado".equals(c.getEstado()))
                .filter(c -> c.getUpdatedAt() != null && c.getUpdatedAt().isBefore(umbral))
                .toList();

        log.info("[SCHEDULER] {} cursos sin actualizar en los últimos {} meses",
                cursosSinActualizar.size(), umbralMesesSinActualizar);

        for (Curso curso : cursosSinActualizar) {
            // Notificar al autor de la última versión
            versionCursoRepository.findTopByCursoIdOrderByNumeroVersionDesc(curso.getId())
                    .ifPresent(version -> {
                        UUID autorId = version.getAutorId();
                        String tituloNotif = "Curso sin actualizar";
                        String mensaje = String.format(
                                "El curso \"%s\" no ha recibido actualizaciones desde %s. "
                                + "Se recomienda revisar su contenido para mantenerlo vigente.",
                                curso.getTitulo(),
                                curso.getUpdatedAt() != null
                                        ? curso.getUpdatedAt().toLocalDate().toString()
                                        : "fecha desconocida"
                        );
                        String link = "/cursos/" + curso.getId() + "/editar";

                        notificacionService.enviarNotificacion(
                                autorId,
                                Notificacion.TIPO_VENCIMIENTO,
                                tituloNotif,
                                mensaje,
                                link
                        );
                        log.info("[SCHEDULER] Notificación de vencimiento enviada a autor={} curso={}",
                                autorId, curso.getId());
                    });
        }

        log.info("[SCHEDULER] Verificación de vencimientos completada.");
    }

    /**
     * Limpia diariamente (3:00 AM) las notificaciones leídas con más de 30 días.
     */
    @Scheduled(cron = "${app.notificaciones.cron-limpieza:0 0 3 * * ?}")
    @Transactional
    public void limpiarNotificacionesAntiguas() {
        log.info("[SCHEDULER] Iniciando limpieza de notificaciones antiguas...");
        int eliminadas = notificacionService.limpiarNotificacionesAntiguas();
        log.info("[SCHEDULER] Limpieza completada: {} notificaciones eliminadas.", eliminadas);
    }

    /**
     * Notifica cada lunes (8:00 AM) a los administradores sobre el estado general
     * de los cursos del sistema.
     */
    @Scheduled(cron = "${app.notificaciones.cron-reporte-semanal:0 0 8 * * MON}")
    @Transactional(readOnly = true)
    public void notificarResumenSemanal() {
        log.info("[SCHEDULER] Generando resumen semanal de cursos...");

        List<Curso> cursosActivos = cursoRepository.findByDeletedAtIsNull(Pageable.unpaged()).getContent();

        long totalPublicados = cursosActivos.stream()
                .filter(c -> "publicado".equals(c.getEstado()))
                .count();

        long totalBorradores = cursosActivos.stream()
                .filter(c -> "borrador".equals(c.getEstado()))
                .count();

        LocalDateTime umbral = LocalDateTime.now().minusMonths(umbralMesesSinActualizar);
        long totalSinActualizar = cursosActivos.stream()
                .filter(c -> "publicado".equals(c.getEstado()))
                .filter(c -> c.getUpdatedAt() != null && c.getUpdatedAt().isBefore(umbral))
                .count();

        log.info("[SCHEDULER] Resumen semanal: publicados={}, borradores={}, sin_actualizar={}",
                totalPublicados, totalBorradores, totalSinActualizar);

        log.info("[SCHEDULER] Resumen semanal generado correctamente.");
    }
}
