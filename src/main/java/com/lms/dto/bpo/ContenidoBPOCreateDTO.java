package com.lms.dto.bpo;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ContenidoBPOCreateDTO {

    @NotBlank(message = "El título es requerido")
    private String titulo;

    private String descripcionCorta;

    @NotBlank(message = "El contenido es requerido")
    private String contenidoHtml;

    private UUID subservicioId;

    private MultipartFile archivo;

    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime vigenciaDesde;

    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime vigenciaHasta;
}
