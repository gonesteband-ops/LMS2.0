package com.lms.controller;

import com.lms.dto.ConsentimientoRequest;
import com.lms.service.ConsentimientoService;
import com.lms.util.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/consentimiento")
public class ConsentimientoController {

    private final ConsentimientoService consentimientoService;
    private final SecurityUtils securityUtils;

    public ConsentimientoController(ConsentimientoService consentimientoService, SecurityUtils securityUtils) {
        this.consentimientoService = consentimientoService;
        this.securityUtils = securityUtils;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> otorgar(@Valid @RequestBody ConsentimientoRequest request,
                                                        Authentication authentication,
                                                        HttpServletRequest httpRequest) {
        UUID usuarioId = securityUtils.getCurrentUserId(authentication);
        var registros = consentimientoService.registrarConsentimiento(usuarioId, request, httpRequest);
        Map<String, Object> response = new HashMap<>();
        response.put("status", "ok");
        response.put("count", registros.size());
        response.put("finalidades", request.finalidades().keySet());
        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/revocar/{finalidad}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, String>> revocar(@PathVariable String finalidad,
                                                        Authentication authentication) {
        UUID usuarioId = securityUtils.getCurrentUserId(authentication);
        consentimientoService.revocarConsentimiento(usuarioId, finalidad);
        return ResponseEntity.ok(Map.of("status", "ok", "finalidad", finalidad));
    }

    @PostMapping(consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE, produces = MediaType.TEXT_HTML_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<String> otorgarHtmx(
            @RequestParam(value = "finalidad_CUENTA", defaultValue = "false") boolean cuenta,
            @RequestParam(value = "finalidad_MARKETING", defaultValue = "false") boolean marketing,
            @RequestParam(value = "finalidad_ANALISIS", defaultValue = "false") boolean analisis,
            @RequestParam(value = "finalidad_COMPARTIR_TERCEROS", defaultValue = "false") boolean compartir,
            @RequestParam(value = "versionPolitica", defaultValue = "1.0") String versionPolitica,
            @RequestParam(value = "idioma", defaultValue = "es-CL") String idioma,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        UUID usuarioId = securityUtils.getCurrentUserId(authentication);
        Map<String, Boolean> finalidades = new HashMap<>();
        finalidades.put("CUENTA", cuenta);
        finalidades.put("MARKETING", marketing);
        finalidades.put("ANALISIS", analisis);
        finalidades.put("COMPARTIR_TERCEROS", compartir);
        ConsentimientoRequest request = new ConsentimientoRequest(versionPolitica, idioma, finalidades);
        consentimientoService.registrarConsentimiento(usuarioId, request, httpRequest);

        String successHtml = """
            <div id="consent-success-container" style="padding:var(--spacing-2xl);text-align:center;">
                <div style="width:4rem;height:4rem;border-radius:var(--border-radius-full);background:rgba(16,185,129,0.1);display:flex;align-items:center;justify-content:center;margin:0 auto var(--spacing-lg);">
                    <svg style="width:2rem;height:2rem;color:var(--color-success);" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"/>
                    </svg>
                </div>
                <h2 style="font-size:var(--font-size-xl);font-weight:700;color:var(--text-primary);margin-bottom:var(--spacing-sm);">Preferencias guardadas</h2>
                <p style="font-size:var(--font-size-sm);color:var(--text-secondary);margin-bottom:var(--spacing-xl);">Puedes modificar estas preferencias en cualquier momento desde tu perfil.</p>
                <a href="/dashboard" class="btn btn-primary" style="display:inline-flex;align-items:center;gap:var(--spacing-sm);padding:0.875rem 2rem;">Ir al Panel Principal</a>
            </div>
            """;
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_HTML_VALUE + ";charset=UTF-8")
                .body(successHtml);
    }

    @GetMapping("/mis-preferencias")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Boolean>> misPreferencias(Authentication authentication) {
        UUID usuarioId = securityUtils.getCurrentUserId(authentication);
        return ResponseEntity.ok(consentimientoService.obtenerEstadoConsentimientos(usuarioId));
    }
}
