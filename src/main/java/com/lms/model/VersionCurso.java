package com.lms.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "versiones_curso", indexes = {
    @Index(name = "idx_version_curso", columnList = "curso_id, numero_version")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class VersionCurso {

    @Id
    @GeneratedValue(generator = "UUID")
    @org.hibernate.annotations.GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "id", updatable = false, nullable = false, columnDefinition = "UUID")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "curso_id", nullable = false)
    @JsonIgnore
    private Curso curso;

    @Column(name = "numero_version", nullable = false)
    private int numeroVersion;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String contenido;

    @Column(name = "hash_contenido", nullable = false, length = 64)
    private String hashContenido;

    @Column(name = "hash_version_anterior", length = 64)
    private String hashVersionAnterior;

    @Column(name = "autor_id", nullable = false)
    private UUID autorId;

    @Column(name = "motivo_cambio", columnDefinition = "TEXT")
    private String motivoCambio;

    @Column(name = "hash_sha256", nullable = false, length = 64)
    private String hashSha256;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
