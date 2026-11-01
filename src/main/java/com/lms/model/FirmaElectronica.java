package com.lms.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidad Firma Electrónica - evento vinculante con no repudio.
 * <p>
 * Registra: IP, timestamp, User-Agent, SO, geolocalización, hash encadenado.
 * Cumple Leyes 19.799 y 21.719.
 */
@Entity
@Table(name = "firmas_electronicas", indexes = {
    @Index(name = "idx_firma_usuario", columnList = "usuario_id")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FirmaElectronica {

    @Id
    @GeneratedValue(generator = "UUID")
    @org.hibernate.annotations.GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "id", updatable = false, nullable = false, columnDefinition = "UUID")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "documento_id", nullable = false)
    private Documento documento;

    @Column(name = "ip_origen", nullable = false, length = 45)
    private String ipOrigen;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(name = "user_agent", nullable = false, length = 512)
    private String userAgent;

    @Column(name = "sistema_operativo", length = 100)
    private String sistemaOperativo;

    @Column(length = 100)
    private String geolocalizacion;

    @Column(name = "hash_documento", nullable = false, length = 64)
    private String hashDocumento;

    @Column(name = "hash_anterior", length = 64)
    private String hashAnterior;

    @Column(name = "hash_sha256", nullable = false, length = 64)
    private String hashSha256;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
