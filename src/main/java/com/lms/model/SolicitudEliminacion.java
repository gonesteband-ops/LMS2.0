package com.lms.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "solicitudes_eliminacion", indexes = {
    @Index(name = "idx_solicitudes_usuario", columnList = "usuario_id"),
    @Index(name = "idx_solicitudes_estado", columnList = "estado")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SolicitudEliminacion {

    @Id
    @GeneratedValue(generator = "UUID")
    @org.hibernate.annotations.GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "id", updatable = false, nullable = false, columnDefinition = "UUID")
    private UUID id;

    @Column(name = "usuario_id", nullable = false)
    private UUID usuarioId;

    @Column(nullable = false, length = 20)
    private String estado;

    @Column(name = "fecha_solicitud", nullable = false, updatable = false)
    private LocalDateTime fechaSolicitud;

    @Column(name = "fecha_ejecucion")
    private LocalDateTime fechaEjecucion;

    @Column(name = "fecha_cancelacion")
    private LocalDateTime fechaCancelacion;

    @Column(columnDefinition = "TEXT")
    private String motivo;

    @Column(name = "hash_sha256", nullable = false, length = 64)
    private String hashSha256;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
