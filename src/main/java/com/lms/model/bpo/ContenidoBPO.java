package com.lms.model.bpo;

import jakarta.persistence.*;
import jakarta.validation.constraints.Pattern;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "contenidos_bpo", indexes = {
    @Index(name = "idx_contenido_cuenta", columnList = "cuenta_id"),
    @Index(name = "idx_contenido_estado", columnList = "estado"),
    @Index(name = "idx_contenido_tipo", columnList = "tipo"),
    @Index(name = "idx_contenido_vigencia", columnList = "vigencia_desde, vigencia_hasta")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@SQLRestriction("deleted_at IS NULL")
public class ContenidoBPO {

    @Id
    @GeneratedValue(generator = "UUID")
    @org.hibernate.annotations.GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "id", updatable = false, nullable = false, columnDefinition = "UUID")
    private UUID id;

    @Column(nullable = false, length = 50)
    @Pattern(regexp = "procedimiento|noticia|oferta", message = "Tipo debe ser: procedimiento, noticia u oferta")
    private String tipo;

    @Column(nullable = false, length = 255)
    private String titulo;

    @Column(name = "descripcion_corta", columnDefinition = "TEXT")
    private String descripcionCorta;

    @Column(name = "contenido_html", nullable = false, columnDefinition = "TEXT")
    private String contenidoHtml;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> metadatos;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    @Builder.Default
    private List<String> etiquetas = List.of();

    @Version
    @Column(name = "version", nullable = false)
    private Integer version = 0;

    @Column(nullable = false, length = 20)
    @Pattern(regexp = "borrador|publicado|archivado", message = "Estado debe ser: borrador, publicado o archivado")
    private String estado = "borrador";

    @Column(name = "vigencia_desde")
    private LocalDateTime vigenciaDesde;

    @Column(name = "vigencia_hasta")
    private LocalDateTime vigenciaHasta;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cuenta_id", nullable = false)
    private CuentaBPO cuenta;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subservicio_id")
    private SubServicioBPO subservicio;

    @Column(name = "creado_por", nullable = false)
    private UUID creadoPor;

    @Column(name = "actualizado_por")
    private UUID actualizadoPor;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "deleted_by")
    private UUID deletedBy;

    @Column(length = 500)
    private String reason;

    @Column(name = "archivo_url")
    private String archivoUrl;

    @Column(name = "archivo_nombre")
    private String archivoNombre;

    @Column(name = "archivo_mime")
    private String archivoMime;

    @Column(name = "archivo_tamano")
    private Long archivoTamano;
}
