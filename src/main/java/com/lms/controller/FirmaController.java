package com.lms.controller;

import com.lms.dto.FirmaRequest;
import com.lms.dto.FirmaResponse;
import com.lms.service.FirmaService;
import com.lms.util.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Controlador REST de firma electrónica (Ley 19.799).
 */
@RestController
@RequestMapping("/api/v1/firmas")
public class FirmaController {

    private final FirmaService firmaService;
    private final SecurityUtils securityUtils;

    public FirmaController(FirmaService firmaService, SecurityUtils securityUtils) {
        this.firmaService = firmaService;
        this.securityUtils = securityUtils;
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<FirmaResponse> firmar(@Valid @RequestBody FirmaRequest request,
                                                Authentication authentication,
                                                HttpServletRequest httpRequest) {
        UUID usuarioId = securityUtils.getCurrentUserId(authentication);
        return ResponseEntity.ok(firmaService.firmarDocumento(usuarioId, request, httpRequest));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<java.util.List<com.lms.dto.FirmaHistorialResponse>> listar(Authentication auth) {
        UUID usuarioId = securityUtils.getCurrentUserId(auth);
        return ResponseEntity.ok(firmaService.listarPorUsuario(usuarioId));
    }

}