package com.lms.mapper.bpo;

import com.lms.dto.bpo.ContenidoBPOResponseDTO;
import com.lms.model.bpo.ContenidoBPO;
import org.springframework.stereotype.Component;

@Component
public class ContenidoBPOMapper {

    public ContenidoBPOResponseDTO toResponseDTO(ContenidoBPO contenido) {
        if (contenido == null) return null;

        ContenidoBPOResponseDTO dto = new ContenidoBPOResponseDTO();
        dto.setId(contenido.getId());
        dto.setTitulo(contenido.getTitulo());
        dto.setDescripcionCorta(contenido.getDescripcionCorta());
        dto.setContenidoHtml(contenido.getContenidoHtml());
        dto.setEstado(contenido.getEstado());
        dto.setCreatedAt(contenido.getCreatedAt());

        if (contenido.getSubservicio() != null) {
            dto.setSubservicioId(contenido.getSubservicio().getId());
            dto.setSubservicioNombre(contenido.getSubservicio().getNombre());
        }
        if (contenido.getCuenta() != null) {
            dto.setCuentaId(contenido.getCuenta().getId());
            dto.setCuentaNombre(contenido.getCuenta().getNombre());
        }

        dto.setArchivoUrl(contenido.getArchivoUrl());
        dto.setArchivoNombre(contenido.getArchivoNombre());
        dto.setArchivoMime(contenido.getArchivoMime());
        dto.setArchivoTamano(contenido.getArchivoTamano());

        return dto;
    }
}
