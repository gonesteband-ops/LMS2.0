package com.lms.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Notificación automática para usuarios del sistema.
 * <p>
 * Tipos de notificación:
 * - VENCIMIENTO: Curso próximo a vencer
 * - REPROBACION: Evaluación reprobada
 * - LOGRO: Curso o evaluación aprobada
 * - SISTEMA: Mensaje administrativo del sistema
 * <p>
 * Diseñado para notificaciones internas (in-app) con soporte futuro
 * para envío por email vía spring-boot-starter-mail.
 */
@Entity
@Table(name = "notificaciones", indexes = {
    @Index(name = "idx_notif_usuario", columnList = "usuario_id"),
    @Index(name = "idx_notif_leida", columnList = "leida"),
    @Index(name = "idx_notif_tipo", columnList = "tipo"),
    @Index(name = "idx_notif_created", columnList = "created_at")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Notificacion {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "id", updatable = false, nullable = false, columnDefinition = "UUID")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false, foreignKey = @ForeignKey(name = "fk_notificacion_usuario"))
    private Usuario usuario;

    @Column(nullable = false, length = 20)
    private String tipo;

    @Column(nullable = false, length = 200)
    private String titulo;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String mensaje;

    @Column(length = 500)
    private String link;

    @Column(nullable = false)
    @Builder.Default
    private boolean leida = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;

    /**
     * Marcadores de tipo de notificación como constantes.
     */
    public static final String TIPO_VENCIMIENTO  = "vencimiento";
    public static final String TIPO_REPROBACION  = "reprobacion";
    public static final String TIPO_LOGRO        = "logro";
    public static final String TIPO_SISTEMA      = "sistema";
}
