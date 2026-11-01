package com.lms.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "intentos_certificacion")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class IntentoCertificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "certificacion_id", nullable = false)
    private Certificacion certificacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @JdbcTypeCode(SqlTypes.JSON)
    private String respuestas;

    private Double nota;

    private Boolean aprobado;

    @Column(nullable = false, length = 20)
    private String estado;

    @Column(nullable = false)
    private LocalDateTime fechaInicio;

    private LocalDateTime fechaEntrega;

    @Column(nullable = false, length = 64)
    private String hashSha256;

    private UUID reiniciadoPor;

    @Column(length = 500)
    private String motivoReinicio;

    private LocalDateTime deletedAt;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
