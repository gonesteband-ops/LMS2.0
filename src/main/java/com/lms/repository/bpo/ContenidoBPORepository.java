package com.lms.repository.bpo;

import com.lms.model.bpo.ContenidoBPO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface ContenidoBPORepository extends JpaRepository<ContenidoBPO, UUID> {

    List<ContenidoBPO> findTop5BySubservicioIdAndEstadoAndDeletedAtIsNullOrderByCreatedAtDesc(
            UUID subservicioId, String estado);

    List<ContenidoBPO> findBySubservicioIdAndEstadoAndDeletedAtIsNullOrderByCreatedAtDesc(
            UUID subservicioId, String estado);

    long countBySubservicioIdAndEstadoAndDeletedAtIsNull(UUID subservicioId, String estado);

    @Query("SELECT c FROM ContenidoBPO c WHERE c.subservicio.id = :subId AND c.estado = 'publicado' AND c.deletedAt IS NULL " +
           "AND (LOWER(c.titulo) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(c.descripcionCorta) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(c.contenidoHtml) LIKE LOWER(CONCAT('%', :q, '%'))) " +
           "ORDER BY c.createdAt DESC")
    List<ContenidoBPO> buscarPorSubservicioYTexto(@Param("subId") UUID subId, @Param("q") String q);

    List<ContenidoBPO> findByCuentaIdAndEstadoAndDeletedAtIsNullOrderByCreatedAtDesc(UUID cuentaId, String estado);

    long countByCuentaIdAndEstadoAndDeletedAtIsNull(UUID cuentaId, String estado);
}
