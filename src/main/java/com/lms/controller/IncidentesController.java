package com.lms.controller;

import com.lms.model.IncidenteSeguridad;
import com.lms.service.IncidenteSeguridadService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/incidentes")
public class IncidentesController {

    private final IncidenteSeguridadService incidenteService;

    public IncidentesController(IncidenteSeguridadService incidenteService) {
        this.incidenteService = incidenteService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPERADMIN')")
    public ResponseEntity<List<IncidenteSeguridad>> listar(
            @RequestParam(required = false) String estado) {
        return ResponseEntity.ok(incidenteService.listar(estado));
    }

    @PutMapping("/{id}/estado")
    @PreAuthorize("hasAnyRole('SUPERADMIN')")
    public ResponseEntity<IncidenteSeguridad> actualizarEstado(
            @PathVariable java.util.UUID id,
            @RequestParam String estado) {
        return ResponseEntity.ok(incidenteService.actualizarEstado(id, estado));
    }
}
