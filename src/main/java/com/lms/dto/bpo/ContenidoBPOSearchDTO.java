package com.lms.dto.bpo;

import lombok.*;

import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ContenidoBPOSearchDTO {

    private String q;
    private String tipo;
    private UUID cuentaId;
    private UUID subservicioId;
    private String estado;
    private boolean incluirVencidos;
    private int page;
    private int size;
}
