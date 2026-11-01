package com.lms.dto;

import java.util.List;

public record CargaMasivaUsuariosReport(
        int totalProcesados,
        int exitosos,
        int duplicados,
        int errores,
        List<String> detalleDuplicados,
        List<String> detalleErrores
) {
    public static CargaMasivaUsuariosReport vacio() {
        return new CargaMasivaUsuariosReport(0, 0, 0, 0, List.of(), List.of());
    }
}
