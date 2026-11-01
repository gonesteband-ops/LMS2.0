package com.lms.dto.bpo;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ContenidoBPOResponseDTO {

    private UUID id;
    private String titulo;
    private String descripcionCorta;
    private String contenidoHtml;
    private String estado;
    private LocalDateTime createdAt;

    // Subcarpeta
    private UUID subservicioId;
    private String subservicioNombre;

    // Gestor
    private UUID cuentaId;
    private String cuentaNombre;

    // Archivo
    private String archivoUrl;
    private String archivoNombre;
    private String archivoMime;
    private Long archivoTamano;
}
