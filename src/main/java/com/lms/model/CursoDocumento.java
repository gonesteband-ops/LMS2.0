package com.lms.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entidad join table para la relación N:M entre Curso y Documento.
 * <p>
 * Permite asociar documentos a cursos sin modificar la estructura
 * independiente de cada entidad.
 */
@Entity
@Table(name = "cursos_documentos")
@IdClass(CursoDocumentoId.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CursoDocumento {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "curso_id", nullable = false)
    private Curso curso;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "documento_id", nullable = false)
    private Documento documento;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
