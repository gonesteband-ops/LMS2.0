package com.lms.repository;

import com.lms.model.IntentoEvaluacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface IntentoEvaluacionRepository extends JpaRepository<IntentoEvaluacion, UUID> {

    Optional<IntentoEvaluacion> findByEvaluacionIdAndUsuarioIdAndEstadoAndDeletedAtIsNull(
            UUID evaluacionId, UUID usuarioId, String estado);

    List<IntentoEvaluacion> findByEvaluacionIdAndUsuarioIdAndDeletedAtIsNullOrderByCreatedAtDesc(
            UUID evaluacionId, UUID usuarioId);

    long countByEvaluacionIdAndUsuarioIdAndEstadoAndDeletedAtIsNull(
            UUID evaluacionId, UUID usuarioId, String estado);

    @Query("SELECT i FROM IntentoEvaluacion i WHERE i.evaluacion.id = :evaluacionId " +
           "AND i.usuario.id = :usuarioId AND i.deletedAt IS NULL ORDER BY i.createdAt DESC")
    List<IntentoEvaluacion> findIntentosByEvaluacionAndUsuario(
            @Param("evaluacionId") UUID evaluacionId,
            @Param("usuarioId") UUID usuarioId);

    List<IntentoEvaluacion> findByEvaluacionIdAndEstadoAndDeletedAtIsNull(
            UUID evaluacionId, String estado);
    long countByUsuarioIdAndEstadoNotAndDeletedAtIsNull(UUID usuarioId, String estado);
}