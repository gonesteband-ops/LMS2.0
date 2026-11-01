package com.lms.repository;

import com.lms.model.EventoAuditoria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EventoAuditoriaRepository extends JpaRepository<EventoAuditoria, UUID> {

    Page<EventoAuditoria> findByTipoEventoOrderByCreatedAtDesc(String tipoEvento, Pageable pageable);

    Page<EventoAuditoria> findByActorIdOrderByCreatedAtDesc(UUID actorId, Pageable pageable);

    @Query("SELECT e FROM EventoAuditoria e ORDER BY e.createdAt DESC")
    Page<EventoAuditoria> findAllOrderByCreatedAtDesc(Pageable pageable);

    @Query("SELECT e FROM EventoAuditoria e WHERE e.tipoEvento IN :tipos ORDER BY e.createdAt DESC")
    Page<EventoAuditoria> findByTipoEventoInOrderByCreatedAtDesc(@Param("tipos") List<String> tipos, Pageable pageable);

    @Query(value = "SELECT e.hash_sha256 FROM eventos_auditoria e ORDER BY e.created_at DESC LIMIT 1", nativeQuery = true)
    Optional<String> findUltimoHash();

    /**
     * Búsqueda en tiempo real por:
     * - Código en metadata (DOC001, SCO001, EVA001)
     * - UUID parcial del actor (actor_id::text LIKE %q%)
     * - Email o nombre en metadata
     * - RUT del usuario (via join con usuarios)
     */
     @Query(value = "SELECT e.* FROM eventos_auditoria e " +
            "LEFT JOIN usuarios u ON u.id = e.actor_id " +
            "WHERE e.metadata->>'codigo' ILIKE '%' || :q || '%' " +
            "OR e.metadata->>'nombreOriginal' ILIKE '%' || :q || '%' " +
            "OR e.metadata->>'titulo' ILIKE '%' || :q || '%' " +
            "OR e.tipo_evento ILIKE '%' || :q || '%' " +
            "OR e.accion ILIKE '%' || :q || '%' " +
            "OR CAST(e.actor_id AS TEXT) ILIKE '%' || :q || '%' " +
            "OR REPLACE(REPLACE(u.rut, '.', ''), '-', '') ILIKE '%' || REPLACE(REPLACE(:q, '.', ''), '-', '') || '%' " +
            "OR u.email ILIKE '%' || :q || '%' " +
            "ORDER BY e.created_at DESC",
            countQuery = "SELECT COUNT(*) FROM eventos_auditoria e " +
                         "LEFT JOIN usuarios u ON u.id = e.actor_id " +
                         "WHERE e.metadata->>'codigo' ILIKE '%' || :q || '%' " +
                         "OR e.metadata->>'nombreOriginal' ILIKE '%' || :q || '%' " +
                         "OR e.metadata->>'titulo' ILIKE '%' || :q || '%' " +
                         "OR e.tipo_evento ILIKE '%' || :q || '%' " +
                         "OR e.accion ILIKE '%' || :q || '%' " +
                         "OR CAST(e.actor_id AS TEXT) ILIKE '%' || :q || '%' " +
                         "OR REPLACE(REPLACE(u.rut, '.', ''), '-', '') ILIKE '%' || REPLACE(REPLACE(:q, '.', ''), '-', '') || '%' " +
                         "OR u.email ILIKE '%' || :q || '%'",
           nativeQuery = true)
    Page<EventoAuditoria> buscarEventos(@Param("q") String query, Pageable pageable);
}
