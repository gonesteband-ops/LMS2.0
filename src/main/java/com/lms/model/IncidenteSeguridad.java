package com.lms.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "incidentes_seguridad", indexes = {
    @Index(name = "idx_incidentes_estado", columnList = "estado"),
    @Index(name = "idx_incidentes_fecha", columnList = "fecha_deteccion")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class IncidenteSeguridad {

    @Id
    @GeneratedValue(generator = "UUID")
    @org.hibernate.annotations.GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "id", updatable = false, nullable = false, columnDefinition = "UUID")
    private UUID id;

    @Column(nullable = false, length = 50)
    private String tipo;

    @Column(name = "nivel_riesgo", nullable = false, length = 20)
    private String nivelRiesgo;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "datos_afectados", columnDefinition = "TEXT")
    private String datosAfectados;

    @Column(name = "usuarios_afectados")
    private Integer usuariosAfectados;

    @Column(name = "ip_origen", length = 45)
    private String ipOrigen;

    @Column(name = "fecha_deteccion", nullable = false, updatable = false)
    private LocalDateTime fechaDeteccion;

    @Column(name = "fecha_notificacion_agencia")
    private LocalDateTime fechaNotificacionAgencia;

    @Column(nullable = false, length = 20)
    private String estado;

    @Column(columnDefinition = "TEXT")
    private String evidencia;

    @Column(name = "hash_sha256", nullable = false, length = 64)
    private String hashSha256;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
