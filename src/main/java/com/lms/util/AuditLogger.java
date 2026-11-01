package com.lms.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Logger de auditoría (Event Sourcing).
 * <p>
 * Registra eventos críticos mediante INSERT (prohibido UPDATE/DELETE).
 * Implementa hash encadenado para integridad.
 * <p>
 * ⚠️ Usa SQL nativo (INSERT) para evitar que Hibernate genere un UPDATE
 * espurio durante el flush del contexto de persistencia, el cual sería
 * bloqueado por el trigger prevent_event_mutation() de PostgreSQL.
 */
@Component
public class AuditLogger {

    private final ObjectMapper objectMapper;
    private final AnonymizationService anonymizationService;

    @PersistenceContext
    private EntityManager entityManager;

    public AuditLogger(ObjectMapper objectMapper, AnonymizationService anonymizationService) {
        this.objectMapper = objectMapper;
        this.anonymizationService = anonymizationService;
    }

    /**
     * Registra un evento de auditoría con hash encadenado.
     * Se ejecuta en una transacción independiente (REQUIRES_NEW) y usa
     * SQL nativo para evitar el entity management de Hibernate.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrarEvento(
            String tipoEvento,
            UUID actorId,
            String accion,
            String recursoTipo,
            UUID recursoId,
            Map<String, Object> metadata
    ) {
        try {
            // Obtener último hash para encadenamiento
            String hashAnterior = obtenerUltimoHash();

            boolean tienePii = anonymizationService.containsPII(metadata);
            Map<String, Object> metadataFinal = tienePii
                    ? anonymizationService.anonymizarMetadata(metadata)
                    : metadata;

            String metadataJson = objectMapper.writeValueAsString(metadataFinal);

            String contenido = String.format("%s|%s|%s|%s|%s|%s",
                    tipoEvento, actorId, accion, recursoTipo, recursoId,
                    metadataJson);

            String hashSha256 = HashUtils.sha256Encadenado(contenido, hashAnterior);

            String sql = "INSERT INTO eventos_auditoria " +
                    "(id, tipo_evento, actor_id, accion, recurso_tipo, recurso_id, " +
                    "metadata, hash_anterior, hash_sha256, es_pii, created_at) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?::jsonb, ?, ?, ?, ?)";

            entityManager.createNativeQuery(sql)
                    .setParameter(1, UUID.randomUUID())
                    .setParameter(2, tipoEvento)
                    .setParameter(3, actorId)
                    .setParameter(4, accion)
                    .setParameter(5, recursoTipo)
                    .setParameter(6, recursoId)
                    .setParameter(7, metadataJson)
                    .setParameter(8, hashAnterior)
                    .setParameter(9, hashSha256)
                    .setParameter(10, tienePii)
                    .setParameter(11, LocalDateTime.now())
                    .executeUpdate();

        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error serializando metadata para evento de auditoría", e);
        } catch (Exception e) {
            throw new RuntimeException("Error registrando evento de auditoría", e);
        }
    }

    /**
     * Obtiene el último hash SHA-256 de la cadena de eventos.
     */
    private String obtenerUltimoHash() {
        try {
            return (String) entityManager.createNativeQuery(
                    "SELECT e.hash_sha256 FROM eventos_auditoria e ORDER BY e.created_at DESC LIMIT 1"
            ).getSingleResult();
        } catch (jakarta.persistence.NoResultException e) {
            return null;
        }
    }
}
