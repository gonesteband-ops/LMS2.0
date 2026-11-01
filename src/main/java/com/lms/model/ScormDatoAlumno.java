package com.lms.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "scorm_datos_alumno",
       uniqueConstraints = @UniqueConstraint(columnNames = {"paquete_id", "usuario_id", "intento_id", "clave"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ScormDatoAlumno {

    @Id
    @GeneratedValue(generator = "UUID")
    @org.hibernate.annotations.GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "id", updatable = false, nullable = false, columnDefinition = "UUID")
    private UUID id;

    @Column(name = "paquete_id", nullable = false)
    private UUID paqueteId;

    @Column(name = "usuario_id", nullable = false)
    private UUID usuarioId;

    @Column(name = "intento_id", nullable = false)
    private UUID intentoId;

    @Column(nullable = false, length = 255)
    private String clave;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String valor;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
