package com.lms.controller;

import com.lms.dto.*;
import com.lms.service.CertificacionService;
import com.lms.util.SecurityUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/certificaciones")
public class CertificacionRestController {

    private final CertificacionService certificacionService;
    private final SecurityUtils securityUtils;

    public CertificacionRestController(CertificacionService certificacionService, SecurityUtils securityUtils) {
        this.certificacionService = certificacionService;
        this.securityUtils = securityUtils;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPERADMIN')")
    public ResponseEntity<CertificacionResponse> crear(@RequestBody CertificacionRequest request, Authentication auth) {
        UUID uid = securityUtils.getCurrentUserId(auth);
        return ResponseEntity.ok(certificacionService.crear(request, uid));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPERADMIN')")
    public ResponseEntity<CertificacionResponse> actualizar(@PathVariable UUID id, @RequestBody CertificacionRequest request, Authentication auth) {
        UUID uid = securityUtils.getCurrentUserId(auth);
        return ResponseEntity.ok(certificacionService.actualizar(id, request, uid));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPERADMIN')")
    public ResponseEntity<Void> eliminar(@PathVariable UUID id, Authentication auth) {
        UUID uid = securityUtils.getCurrentUserId(auth);
        certificacionService.eliminar(id, uid);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/preguntas")
    @PreAuthorize("hasAnyRole('SUPERADMIN')")
    public ResponseEntity<List<PreguntaCertificacionResponse>> obtenerPreguntas(@PathVariable UUID id) {
        return ResponseEntity.ok(certificacionService.obtenerPreguntas(id));
    }

    @GetMapping("/{id}/rendir")
    @PreAuthorize("hasRole('ESTUDIANTE')")
    public ResponseEntity<List<PreguntaCertificacionResponse>> rendir(@PathVariable UUID id, Authentication auth) {
        UUID uid = securityUtils.getCurrentUserId(auth);
        var inscripcion = certificacionService.listarInscritos(id).stream()
                .filter(i -> i.getUsuario().getId().equals(uid)).findFirst();
        if (inscripcion.isEmpty()) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(certificacionService.obtenerPreguntasSanitizadas(id));
    }

    @PostMapping("/{id}/iniciar")
    @PreAuthorize("hasRole('ESTUDIANTE')")
    public ResponseEntity<?> iniciar(@PathVariable UUID id, Authentication auth) {
        UUID uid = securityUtils.getCurrentUserId(auth);
        try {
            return ResponseEntity.ok(certificacionService.iniciarIntento(id, uid));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/responder")
    @PreAuthorize("hasRole('ESTUDIANTE')")
    public ResponseEntity<?> responder(@PathVariable UUID id, @RequestBody Map<String, Object> respuestas, Authentication auth) {
        UUID uid = securityUtils.getCurrentUserId(auth);
        try {
            var enCurso = certificacionService.buscarIntentoEnCurso(id, uid);
            if (enCurso == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "No hay un intento en curso"));
            }
            return ResponseEntity.ok(certificacionService.entregarIntento(enCurso.getId(), respuestas, uid));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/resultados")
    @PreAuthorize("hasRole('ESTUDIANTE')")
    public ResponseEntity<List<IntentoCertificacionResponse>> resultados(Authentication auth) {
        UUID uid = securityUtils.getCurrentUserId(auth);
        var misCerts = certificacionService.listarMisCertificaciones(uid);
        return ResponseEntity.ok(misCerts.stream()
                .flatMap(c -> certificacionService.resultados(c.getId(), uid).stream())
                .toList());
    }

    @GetMapping("/{id}/resultado-detalle")
    @PreAuthorize("hasRole('ESTUDIANTE')")
    public ResponseEntity<List<IntentoCertificacionResponse>> resultadoDetalle(@PathVariable UUID id, Authentication auth) {
        UUID uid = securityUtils.getCurrentUserId(auth);
        return ResponseEntity.ok(certificacionService.resultados(id, uid));
    }
}
