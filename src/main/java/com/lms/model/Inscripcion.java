package com.lms.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidad que registra la inscripción/participación de un usuario en un curso.
 * <p>
 * Cumple con Ley 19.799 al mantener trazabilidad completa de quién,
 * cuándo y en qué estado se encuentra cada inscripción.
 */
@Entity
@Table(name = "inscripciones", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"curso_id", "usuario_id", "deleted_at"})
}, indexes = {
    @Index(name = "idx_inscripcion_curso", columnList = "curso_id"),
    @Index(name = "idx_inscripcion_usuario", columnList = "usuario_id"),
    @Index(name = "idx_inscripcion_completado", columnList = "completado"),
    @Index(name = "idx_inscripcion_fecha_limite", columnList = "fecha_limite")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Inscripcion {

    @Id
    @GeneratedValue(generator = "UUID")
    @org.hibernate.annotations.GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "id", updatable = false, nullable = false, columnDefinition = "UUID")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "curso_id", nullable = false)
    @JsonIgnore
    private Curso curso;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    @JsonIgnore
    private Usuario usuario;

    @Column(name = "fecha_inscripcion")
    private LocalDateTime fechaInscripcion;

    @Column(name = "fecha_limite")
    private LocalDateTime fechaLimite;

    @Column(nullable = false)
    private boolean completado = false;

    @Column(name = "nota")
    private Double nota;

    @Column(name = "fecha_completado")
    private LocalDateTime fechaCompletado;

    @Column(name = "hash_sha256", nullable = false, length = 64)
    private String hashSha256;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "deleted_by")
    private UUID deletedBy;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    // ============ CAMPOS DE FIRMA DIGITAL DE CUMPLIMIENTO (Tarea 1) ============

    @Column(name = "declaracion_jurada_aceptada", nullable = false)
    private boolean declaracionJuradaAceptada = false;

    @Column(name = "ip_firma", length = 45)
    private String ipFirma;

    @Column(name = "timestamp_firma")
    private LocalDateTime timestampFirma;

    @Column(name = "firma_hash", length = 64)
    private String firmaHash;

    // ============ CAMPOS DE TRAZABILIDAD FORZADO POR SUPERVISOR (Tarea 2) ============

    @Column(name = "forzado_por_supervisor", nullable = false)
    private boolean forzadoPorSupervisor = false;

    @Column(name = "motivo_forzado", columnDefinition = "TEXT")
    private String motivoForzado;

    @Column(name = "comentario_forzado", columnDefinition = "TEXT")
    private String comentarioForzado;

    @Column(name = "forzado_por_usuario_id", columnDefinition = "UUID")
    private UUID forzadoPorUsuarioId;

    @Column(name = "forzado_en")
    private LocalDateTime forzadoEn;

    @Column(name = "etiqueta_forzado", length = 100)
    private String etiquetaForzado;
}
