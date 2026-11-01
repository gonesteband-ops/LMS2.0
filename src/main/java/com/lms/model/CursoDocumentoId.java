package com.lms.model;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.UUID;

/**
 * ID compuesto para la relación N:M entre Curso y Documento.
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @EqualsAndHashCode
public class CursoDocumentoId implements Serializable {

    private UUID curso;
    private UUID documento;
}
