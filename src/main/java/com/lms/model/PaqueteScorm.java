package com.lms.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "paquetes_scorm")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PaqueteScorm {

    @Id
    @GeneratedValue(generator = "UUID")
    @org.hibernate.annotations.GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "id", updatable = false, nullable = false, columnDefinition = "UUID")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "curso_id", nullable = false)
    private Curso curso;

    @Column(nullable = false, length = 255)
    private String titulo;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "version_scorm", nullable = false, length = 10)
    private String versionScorm;

    @Column(name = "url_zip", nullable = false, length = 512)
    private String urlZip;

    @Column(name = "url_index", nullable = false, length = 512)
    private String urlIndex;

    @Column(name = "hash_sha256", nullable = false, length = 64)
    private String hashSha256;

    @Column(name = "tamano_bytes", nullable = false)
    private long tamanoBytes;

    @Column(length = 20)
    private String codigo;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "deleted_by")
    private UUID deletedBy;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Column(nullable = false)
    private boolean habilitado = true;
}
