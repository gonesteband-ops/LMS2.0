package com.lms.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class IntentoCertificacionResponse {
    private UUID id;
    private UUID certificacionId;
    private UUID usuarioId;
    private String respuestas;
    private Double nota;
    private Boolean aprobado;
    private String estado;
    private LocalDateTime fechaInicio;
    private LocalDateTime fechaEntrega;
    private LocalDateTime createdAt;
}
