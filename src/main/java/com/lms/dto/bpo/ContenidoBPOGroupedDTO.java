package com.lms.dto.bpo;

import java.util.UUID;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ContenidoBPOGroupedDTO {
    private String subservicioNombre;
    private UUID subservicioId;
    private long total;
    private java.util.List<ContenidoBPOResponseDTO> contenidos;
}
