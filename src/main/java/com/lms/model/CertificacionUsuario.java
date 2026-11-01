package com.lms.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "certificaciones_usuarios",
       uniqueConstraints = @UniqueConstraint(columnNames = {"certificacion_id", "usuario_id"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@SQLRestriction("deleted_at IS NULL")
public class CertificacionUsuario {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "certificacion_id", nullable = false)
    private Certificacion certificacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    private Double nota;

    private Boolean aprobado;

    @Column(nullable = false)
    private Boolean completado;

    @Column(length = 64)
    private String hashSha256;

    private LocalDateTime deletedAt;

    private UUID deletedBy;

    @Column(length = 500)
    private String reason;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
