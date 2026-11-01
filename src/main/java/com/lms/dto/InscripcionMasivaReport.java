package com.lms.dto;

import java.util.List;

/**
 * Reporte detallado de una inscripción masiva desde archivo CSV/XLSX.
 * <p>
 * Contiene el desglose por escenario:
 * <ul>
 *   <li><strong>exitosos</strong> — usuarios que fueron inscritos correctamente</li>
 *   <li><strong>noEncontrados</strong> — valores (email/UUID) que no corresponden a ningún usuario activo</li>
 *   <li><strong>yaInscritos</strong> — usuarios que ya estaban inscritos en el curso</li>
 *   <li><strong>errores</strong> — errores inesperados (formato de línea inválido, etc.)</li>
 * </ul>
 */
public record InscripcionMasivaReport(
        int totalProcesados,
        int exitosos,
        int noEncontrados,
        int yaInscritos,
        int errores,
        List<InscripcionResponse> inscripciones,
        List<String> detalleNoEncontrados,
        List<String> detalleYaInscritos,
        List<String> detalleErrores
) {

    /**
     * Crea un reporte vacío (sin procesar nada).
     */
    public static InscripcionMasivaReport vacio() {
        return new InscripcionMasivaReport(0, 0, 0, 0, 0,
                List.of(), List.of(), List.of(), List.of());
    }
}
