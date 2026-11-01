package com.lms.repository;

import com.lms.model.Notificacion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface NotificacionRepository extends JpaRepository<Notificacion, UUID> {

    /**
     * Lista las notificaciones de un usuario ordenadas por fecha descendente.
     */
    Page<Notificacion> findByUsuarioIdOrderByCreatedAtDesc(UUID usuarioId, Pageable pageable);

    /**
     * Lista las notificaciones NO leídas de un usuario.
     */
    List<Notificacion> findByUsuarioIdAndLeidaFalseOrderByCreatedAtDesc(UUID usuarioId);

    /**
     * Cuenta las notificaciones NO leídas de un usuario.
     */
    long countByUsuarioIdAndLeidaFalse(UUID usuarioId);

    /**
     * Lista notificaciones por tipo.
     */
    Page<Notificacion> findByUsuarioIdAndTipoOrderByCreatedAtDesc(UUID usuarioId, String tipo, Pageable pageable);

    /**
     * Marca todas las notificaciones de un usuario como leídas.
     */
    @Modifying
    @Query("UPDATE Notificacion n SET n.leida = true WHERE n.usuario.id = :usuarioId AND n.leida = false")
    int marcarTodasComoLeidas(@Param("usuarioId") UUID usuarioId);

    /**
     * Elimina notificaciones leídas con más de 30 días de antigüedad.
     */
    @Modifying
    @Query(value = "DELETE FROM notificaciones WHERE leida = true AND created_at < NOW() - INTERVAL '30 days'", nativeQuery = true)
    int limpiarNotificacionesAntiguas();
}
