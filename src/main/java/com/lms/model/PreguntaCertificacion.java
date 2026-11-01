package com.lms.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "preguntas_certificacion")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PreguntaCertificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "certificacion_id", nullable = false)
    private Certificacion certificacion;

    @Column(nullable = false, length = 50)
    private String tipo;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String contenido;

    @Column(nullable = false)
    private Double peso;

    @Column(columnDefinition = "TEXT")
    private String opciones;

    @Column(name = "respuesta_correcta", columnDefinition = "TEXT")
    private String respuestaCorrecta;

    @Column(length = 500)
    private String multimediaUrl;

    @Column(nullable = false, length = 64)
    private String hashSha256;

    @Column(nullable = false)
    private Integer orden;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private LocalDateTime deletedAt;
}
