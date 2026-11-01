package com.lms.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.type.SqlTypes;
import org.hibernate.annotations.JdbcTypeCode;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Event Sourcing: registro inmutable de eventos críticos.
 * ⚠️ SOLO INSERT - Prohibido UPDATE o DELETE.
 * Implementa hash encadenado para integridad de la cadena.
 */
@Entity
@Table(name = "eventos_auditoria", indexes = {
    @Index(name = "idx_eventos_tipo", columnList = "tipo_evento"),
    @Index(name = "idx_eventos_actor", columnList = "actor_id"),
    @Index(name = "idx_eventos_created", columnList = "created_at")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EventoAuditoria {

    @Id
    @GeneratedValue(generator = "UUID")
    @org.hibernate.annotations.GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "id", updatable = false, nullable = false, columnDefinition = "UUID")
    private UUID id;

    @Column(name = "tipo_evento", nullable = false, length = 50)
    private String tipoEvento;

    @Column(name = "actor_id")
    private UUID actorId;

    @Column(nullable = false, length = 100)
    private String accion;

    @Column(name = "recurso_tipo", length = 50)
    private String recursoTipo;

    @Column(name = "recurso_id")
    private UUID recursoId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> metadata;

    @Column(name = "hash_anterior", length = 64)
    private String hashAnterior;

    @Column(name = "hash_sha256", nullable = false, length = 64)
    private String hashSha256;

    @Column(name = "es_pii", nullable = false, columnDefinition = "BOOLEAN NOT NULL DEFAULT FALSE")
    private boolean esPii = false;

    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;
}
