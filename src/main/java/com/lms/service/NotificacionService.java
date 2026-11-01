package com.lms.service;

import com.lms.model.Notificacion;
import com.lms.model.Usuario;
import com.lms.repository.NotificacionRepository;
import com.lms.repository.UsuarioRepository;
import com.lms.util.AuditLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Servicio de notificaciones automáticas del sistema.
 * <p>
 * Gestiona el envío, consulta y marcado de notificaciones
 * internas (in-app). Prepara la infraestructura para envío
 * por email vía spring-boot-starter-mail.
 */
@Service
@Transactional
public class NotificacionService {

    private static final Logger log = LoggerFactory.getLogger(NotificacionService.class);

    private final NotificacionRepository notificacionRepository;
    private final UsuarioRepository usuarioRepository;
    private final AuditLogger auditLogger;

    public NotificacionService(NotificacionRepository notificacionRepository,
                               UsuarioRepository usuarioRepository,
                               AuditLogger auditLogger) {
        this.notificacionRepository = notificacionRepository;
        this.usuarioRepository = usuarioRepository;
        this.auditLogger = auditLogger;
    }

    /**
     * Envía una notificación a un usuario.
     *
     * @param usuarioId UUID del usuario destinatario
     * @param tipo      Tipo de notificación (vencimiento, reprobacion, logro, sistema)
     * @param titulo    Título breve de la notificación
     * @param mensaje   Cuerpo del mensaje
     * @param link      Enlace opcional a recurso relacionado
     * @return La notificación creada
     */
    public Notificacion enviarNotificacion(UUID usuarioId, String tipo, String titulo, String mensaje, String link) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + usuarioId));

        Notificacion notificacion = Notificacion.builder()
                .usuario(usuario)
                .tipo(tipo)
                .titulo(titulo)
                .mensaje(mensaje)
                .link(link)
                .leida(false)
                .build();

        Notificacion guardada = notificacionRepository.save(notificacion);

        log.info("Notificación enviada a usuario={} tipo={} titulo={}", usuarioId, tipo, titulo);

        auditLogger.registrarEvento(
                "NOTIFICACION_ENVIADA",
                usuarioId,
                "Envío de notificación",
                "Notificacion",
                guardada.getId(),
                Map.of("tipo", tipo, "titulo", titulo, "mensaje", mensaje)
        );

        return guardada;
    }

    /**
     * Envía una notificación sin enlace.
     */
    public Notificacion enviarNotificacion(UUID usuarioId, String tipo, String titulo, String mensaje) {
        return enviarNotificacion(usuarioId, tipo, titulo, mensaje, null);
    }

    /**
     * Marca una notificación como leída.
     */
    public void marcarComoLeida(UUID notificacionId) {
        notificacionRepository.findById(notificacionId).ifPresent(n -> {
            n.setLeida(true);
            notificacionRepository.save(n);
        });
    }

    /**
     * Marca todas las notificaciones de un usuario como leídas.
     */
    @Transactional
    public int marcarTodasComoLeidas(UUID usuarioId) {
        int actualizadas = notificacionRepository.marcarTodasComoLeidas(usuarioId);
        if (actualizadas > 0) {
            log.info("{} notificaciones marcadas como leídas para usuario={}", actualizadas, usuarioId);
        }
        return actualizadas;
    }

    /**
     * Lista las notificaciones no leídas de un usuario.
     */
    @Transactional(readOnly = true)
    public List<Notificacion> listarNoLeidas(UUID usuarioId) {
        return notificacionRepository.findByUsuarioIdAndLeidaFalseOrderByCreatedAtDesc(usuarioId);
    }

    /**
     * Lista las notificaciones de un usuario con paginación.
     */
    @Transactional(readOnly = true)
    public Page<Notificacion> listar(UUID usuarioId, Pageable pageable) {
        return notificacionRepository.findByUsuarioIdOrderByCreatedAtDesc(usuarioId, pageable);
    }

    /**
     * Cuenta las notificaciones no leídas de un usuario.
     */
    @Transactional(readOnly = true)
    public long contarNoLeidas(UUID usuarioId) {
        return notificacionRepository.countByUsuarioIdAndLeidaFalse(usuarioId);
    }

    /**
     * Limpia notificaciones leídas con más de 30 días de antigüedad.
     * Este método es invocado por la tarea programada @Scheduled.
     */
    @Transactional
    public int limpiarNotificacionesAntiguas() {
        int eliminadas = notificacionRepository.limpiarNotificacionesAntiguas();
        if (eliminadas > 0) {
            log.info("{} notificaciones antiguas eliminadas", eliminadas);
        }
        return eliminadas;
    }
}
