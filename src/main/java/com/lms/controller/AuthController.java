package com.lms.controller;

import com.lms.dto.*;
import com.lms.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * Controlador REST de autenticación.
 * <p>
 * Endpoints públicos para registro, login, refresh de tokens JWT y MFA TOTP.
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.registrar(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/register", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE, produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> registerHtmx(
            @RequestParam("nombreCompleto") String nombreCompleto,
            @RequestParam("email") String email,
            @RequestParam("password") String password,
            @RequestParam("confirmarPassword") String confirmarPassword,
            @RequestParam(value = "fechaNacimiento", required = false) String fechaNacimientoStr) {
        try {
            LocalDate fechaNacimiento = (fechaNacimientoStr == null || fechaNacimientoStr.isBlank())
                    ? null : LocalDate.parse(fechaNacimientoStr);
            RegisterRequest request = new RegisterRequest(
                    nombreCompleto, email, password, confirmarPassword, fechaNacimiento);
            authService.registrar(request);
            String successHtml = "<script>window.location.href='/consentimiento'</script>";
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_HTML_VALUE + ";charset=UTF-8")
                    .body(successHtml);
        } catch (IllegalArgumentException e) {
            String errorHtml = "<div id=\"form-response\" style=\"padding:var(--spacing-md);background:#fef2f2;border:1px solid #fecaca;border-radius:var(--border-radius-lg);font-size:var(--font-size-sm);color:#991b1b;\" role=\"alert\">"
                    + e.getMessage().replaceAll("<", "&lt;").replaceAll(">", "&gt;")
                    + "</div>";
            return ResponseEntity.unprocessableEntity()
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_HTML_VALUE + ";charset=UTF-8")
                    .body(errorHtml);
        }
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.autenticar(request);
        // Si requiere MFA, no seteamos cookie del access token
        if (response.requiresMfa()) {
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, criarCookieToken(response.accessToken()))
                .body(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@RequestHeader("Authorization") String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.badRequest().build();
        }
        String refreshToken = authHeader.substring(7);
        AuthResponse response = authService.refreshToken(refreshToken);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/mfa/verificar")
    public ResponseEntity<AuthResponse> verificarMfa(@Valid @RequestBody MfaVerificarRequest request) {
        AuthResponse response = authService.verificarMfa(request.tokenTemporal(), request.codigo());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, criarCookieToken(response.accessToken()))
                .body(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, "access_token=; Max-Age=0; Path=/; HttpOnly; SameSite=Strict")
                .build();
    }

    private String criarCookieToken(String token) {
        return String.format("access_token=%s; Max-Age=%d; Path=/; HttpOnly; SameSite=Strict",
                token, 900); // 15 minutos
    }
}
