package com.lms.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "consentimientos", indexes = {
    @Index(name = "idx_consentimiento_usuario", columnList = "usuario_id"),
    @Index(name = "idx_consentimiento_finalidad", columnList = "usuario_id, finalidad")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Consentimiento {

    public enum Finalidad {
        CUENTA, MARKETING, ANALISIS, COMPARTIR_TERCEROS
    }

    @Id
    @GeneratedValue(generator = "UUID")
    @org.hibernate.annotations.GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "id", updatable = false, nullable = false, columnDefinition = "UUID")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "ip_origen", nullable = false, length = 45)
    private String ipOrigen;

    @Column(name = "user_agent", nullable = false, length = 512)
    private String userAgent;

    @Column(name = "hash_documento", nullable = false, length = 64)
    private String hashDocumento;

    @Column(name = "version_politica", nullable = false, length = 20)
    private String versionPolitica;

    @Column(nullable = false, length = 10)
    private String idioma;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private Finalidad finalidad;

    @Column(nullable = false, columnDefinition = "BOOLEAN NOT NULL DEFAULT TRUE")
    private boolean activo;

    @Column(name = "fecha_revocacion")
    private LocalDateTime fechaRevocacion;

    @Column(nullable = false)
    private boolean aceptado;

    @Column(name = "hash_sha256", nullable = false, length = 64)
    private String hashSha256;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
