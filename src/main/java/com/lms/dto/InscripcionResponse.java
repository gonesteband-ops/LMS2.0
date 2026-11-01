package com.lms.dto;

import com.lms.model.Inscripcion;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO de respuesta para inscripciones.
 */
public record InscripcionResponse(
        UUID id,
        UUID cursoId,
        String cursoTitulo,
        String cursoCategoria,
        String cursoEstado,
        boolean cursoRequiereFirma,
        UUID usuarioId,
        String usuarioNombre,
        String usuarioEmail,
        LocalDateTime fechaInscripcion,
        LocalDateTime fechaLimite,
        boolean completado,
        Double nota,
        LocalDateTime fechaCompletado,
        long diasRestantes,
        boolean declaracionJuradaAceptada,
        String ipFirma,
        LocalDateTime timestampFirma,
        boolean forzadoPorSupervisor,
        String motivoForzado,
        String etiquetaForzado
) {
    public static InscripcionResponse fromEntity(Inscripcion i) {
        long diasRestantes = 0;
        if (i.getFechaLimite() != null && !i.isCompletado()) {
            diasRestantes = java.time.Duration.between(
                LocalDateTime.now(), i.getFechaLimite()
            ).toDays();
        }

        String cursoEstado = i.getCurso().getEstado() != null ? i.getCurso().getEstado() : "borrador";
        return new InscripcionResponse(
            i.getId(),
            i.getCurso().getId(),
            i.getCurso().getTitulo(),
            i.getCurso().getCategoria(),
            cursoEstado,
            i.getCurso().isRequiereFirmaCumplimiento(),
            i.getUsuario().getId(),
            i.getUsuario().getNombreCompleto(),
            i.getUsuario().getEmail(),
            i.getFechaInscripcion(),
            i.getFechaLimite(),
            i.isCompletado(),
            i.getNota(),
            i.getFechaCompletado(),
            Math.max(0, diasRestantes),
            i.isDeclaracionJuradaAceptada(),
            i.getIpFirma(),
            i.getTimestampFirma(),
            i.isForzadoPorSupervisor(),
            i.getMotivoForzado(),
            i.getEtiquetaForzado()
        );
    }
}
