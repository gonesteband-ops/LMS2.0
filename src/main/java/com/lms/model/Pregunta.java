package com.lms.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "preguntas", indexes = {
    @Index(name = "idx_pregunta_evaluacion", columnList = "evaluacion_id")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Pregunta {

    @Id
    @GeneratedValue(generator = "UUID")
    @org.hibernate.annotations.GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "id", updatable = false, nullable = false, columnDefinition = "UUID")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evaluacion_id", nullable = false)
    @JsonIgnore
    private Evaluacion evaluacion;

    @Column(nullable = false, length = 30)
    private String tipo;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String contenido;

    @Column(nullable = false)
    private double peso = 1.0;

    @Column(name = "tiempo_respuesta_ms")
    private Integer tiempoRespuestaMs;

    @Column(columnDefinition = "TEXT")
    private String opciones;

    @Column(name = "respuesta_correcta", columnDefinition = "TEXT")
    private String respuestaCorrecta;

    @Column(name = "multimedia_url", length = 512)
    private String multimediaUrl;

    @Column(name = "hash_sha256", nullable = false, length = 64)
    private String hashSha256;

    @Column(nullable = false)
    private int orden;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}
