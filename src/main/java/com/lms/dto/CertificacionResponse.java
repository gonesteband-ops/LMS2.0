package com.lms.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CertificacionResponse {
    private UUID id;
    private String titulo;
    private String descripcion;
    private Double pesoTotal;
    private Double notaMinima;
    private Integer tiempoEstimadoMinutos;
    private String configuracion;
    private String codigo;
    private String hashSha256;
    private Boolean activo;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<PreguntaCertificacionResponse> preguntas;
}
