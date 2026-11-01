package com.lms.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "exportaciones", indexes = {
    @Index(name = "idx_exportacion_solicitante", columnList = "solicitanteId"),
    @Index(name = "idx_exportacion_estado", columnList = "estado")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Exportacion {

    @Id
    @GeneratedValue(generator = "UUID")
    @org.hibernate.annotations.GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "id", updatable = false, nullable = false, columnDefinition = "UUID")
    private UUID id;

    @Column(name = "solicitante_id", nullable = false)
    private UUID solicitanteId;

    @Column(name = "tipo_reporte", nullable = false, length = 50)
    private String tipoReporte;

    @Column(columnDefinition = "TEXT")
    private String filtros;

    @Column(nullable = false, length = 20)
    private String estado = "pendiente";

    @Column(name = "url_resultado", length = 512)
    private String urlResultado;

    @Column(name = "task_id_celery", length = 255)
    private String taskIdCelery;

    @Column(name = "error_mensaje", columnDefinition = "TEXT")
    private String errorMensaje;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Transient
    public String getNombreReporte() {
        String tipo = tipoReporte != null ? tipoReporte : "";
        String f = filtros != null ? filtros.trim() : "";

        if ("usuarios".equals(tipo)) {
            if (f.contains("\"estado\":\"en-proceso\"")) return "Usuarios - En proceso de eliminación";
            if (f.contains("\"estado\":\"eliminados\"")) return "Usuarios - Eliminados";
            if (f.contains("\"rol\":\"ESTUDIANTE\"")) return "Usuarios - Estudiantes";
            if (f.contains("\"rol\":\"INSTRUCTOR\"")) return "Usuarios - Instructores";
            if (f.contains("\"rol\":\"ADMIN\"")) return "Usuarios - Administradores";
            if (f.contains("\"activo\":true")) return "Usuarios - Solo activos";
            return "Usuarios - Todos";
        }

        if (tipo.isEmpty()) return "—";
        return tipo.substring(0, 1).toUpperCase() + tipo.substring(1);
    }
}
