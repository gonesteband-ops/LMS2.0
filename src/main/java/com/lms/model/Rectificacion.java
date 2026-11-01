package com.lms.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "rectificaciones", indexes = {
    @Index(name = "idx_rectificaciones_usuario", columnList = "usuario_id")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Rectificacion {

    @Id
    @GeneratedValue(generator = "UUID")
    @org.hibernate.annotations.GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "id", updatable = false, nullable = false, columnDefinition = "UUID")
    private UUID id;

    @Column(name = "usuario_id", nullable = false)
    private UUID usuarioId;

    @Column(name = "campo_modificado", nullable = false, length = 50)
    private String campoModificado;

    @Column(name = "valor_anterior", columnDefinition = "TEXT")
    private String valorAnterior;

    @Column(name = "valor_nuevo", nullable = false, columnDefinition = "TEXT")
    private String valorNuevo;

    @Column(name = "ip_origen", length = 45)
    private String ipOrigen;

    @Column(name = "hash_sha256", nullable = false, length = 64)
    private String hashSha256;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
