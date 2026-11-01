package com.lms.controller;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class PlantillaController {

    @GetMapping("/plantilla-inscripcion")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Resource> descargarPlantilla() {
        String csv = "usuarioId\n16.478.280-8\n12.345.678-9\n";
        byte[] bytes = csv.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        ByteArrayResource resource = new ByteArrayResource(bytes);
        return ResponseEntity.ok()
                .header("Content-Type", "text/csv;charset=UTF-8")
                .header("Content-Disposition", "attachment; filename=plantilla-inscripcion.csv")
                .body(resource);
    }
}
