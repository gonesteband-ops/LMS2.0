package com.lms.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidad Permiso - permiso granular sobre recurso+acción.
 * <p>
 * Ejemplo: recurso="curso", accion="CREAR" → nombre "CURSO_CREAR"
 */
@Entity
@Table(name = "permisos")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Permiso {

    @Id
    @GeneratedValue(generator = "UUID")
    @org.hibernate.annotations.GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "id", updatable = false, nullable = false, columnDefinition = "UUID")
    private UUID id;

    @Column(nullable = false, unique = true, length = 100)
    private String nombre;

    @Column(nullable = false, length = 50)
    private String recurso;

    @Column(nullable = false, length = 50)
    private String accion;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
