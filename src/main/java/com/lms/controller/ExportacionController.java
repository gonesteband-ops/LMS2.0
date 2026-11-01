package com.lms.controller;

import com.lms.model.Exportacion;
import com.lms.service.ExportacionService;
import com.lms.util.SecurityUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Controlador REST de exportaciones (reportes XLSX).
 */
@RestController
@RequestMapping("/api/v1/exportaciones")
public class ExportacionController {

    private final ExportacionService exportacionService;
    private final SecurityUtils securityUtils;

    public ExportacionController(ExportacionService exportacionService, SecurityUtils securityUtils) {
        this.exportacionService = exportacionService;
        this.securityUtils = securityUtils;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'INSTRUCTOR')")
    public ResponseEntity<Exportacion> solicitar(@RequestParam String tipoReporte,
                                                 @RequestParam(required = false) String filtros,
                                                 Authentication authentication) {
        UUID solicitanteId = securityUtils.getCurrentUserId(authentication);
        return ResponseEntity.ok(exportacionService.solicitarExportacion(solicitanteId, tipoReporte, filtros));
    }

    @GetMapping("/{id}/status")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> obtenerEstado(@PathVariable UUID id) {
        return exportacionService.obtenerEstado(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Exportacion>> listar(Authentication authentication) {
        UUID solicitanteId = securityUtils.getCurrentUserId(authentication);
        return ResponseEntity.ok(exportacionService.listarPorSolicitante(solicitanteId));
    }

    @DeleteMapping
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'INSTRUCTOR')")
    public ResponseEntity<Map<String, Object>> limpiarHistorial(Authentication authentication) {
        UUID solicitanteId = securityUtils.getCurrentUserId(authentication);
        int count = exportacionService.limpiarHistorial(solicitanteId);
        return ResponseEntity.ok(Map.of(
            "eliminados", count,
            "mensaje", "Historial limpiado: " + count + " exportaciones eliminadas."
        ));
    }

    /**
     * Descarga un reporte XLSX generado previamente.
     *
     * @param id     UUID de la exportación
     * @param object nombre del objeto en MinIO (ej: "exportaciones/evaluaciones_xxx.xlsx")
     */
    @GetMapping("/{id}/descargar")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<byte[]> descargar(@PathVariable UUID id,
                                            @RequestParam String object,
                                            Authentication authentication) {
        try {
            byte[] reporteBytes = exportacionService.descargarReporte(object);

            // Extraer nombre del archivo desde la ruta del objeto
            String filename = object.contains("/")
                    ? object.substring(object.lastIndexOf('/') + 1)
                    : object;

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(reporteBytes);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/ejemplo")
    @PreAuthorize("hasAnyRole('SUPERADMIN')")
    public ResponseEntity<byte[]> descargarEjemplo() {
        String[] headers = {"ID", "Nombre", "Email", "Curso", "Estado", "Fecha"};
        List<String[]> data = Collections.singletonList(
                new String[]{"1", "Ejemplo", "ejemplo@lms.cl", "Curso Demo", "Completado", "2026-01-01"}
        );

        byte[] excel = exportacionService.generarExcelEjemplo(headers, data);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=reporte_ejemplo.xlsx")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(excel);
    }
}
