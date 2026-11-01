package com.lms.repository;

import com.lms.model.Exportacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface ExportacionRepository extends JpaRepository<Exportacion, UUID> {
    List<Exportacion> findBySolicitanteIdOrderByCreatedAtDesc(UUID solicitanteId);

    List<Exportacion> findBySolicitanteIdAndDeletedAtIsNullOrderByCreatedAtDesc(UUID solicitanteId);

    List<Exportacion> findByEstadoOrderByCreatedAtDesc(String estado);

    long countBySolicitanteId(UUID solicitanteId);

    @Modifying
    @Query("UPDATE Exportacion e SET e.deletedAt = :now WHERE e.solicitanteId = :solicitanteId AND e.deletedAt IS NULL")
    int softDeleteBySolicitanteId(@Param("solicitanteId") UUID solicitanteId, @Param("now") LocalDateTime now);
}
