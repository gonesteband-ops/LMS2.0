package com.lms.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PreguntaCertificacionResponse {
    private UUID id;
    private UUID certificacionId;
    private String tipo;
    private String contenido;
    private Double peso;
    private String opciones;
    private String respuestaCorrecta;
    private String multimediaUrl;
    private Integer orden;
    private LocalDateTime createdAt;
}
