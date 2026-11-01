package com.lms.service;

import com.lms.dto.AuthResponse;
import com.lms.dto.LoginRequest;
import com.lms.dto.RegisterRequest;
import com.lms.model.Usuario;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Servicio de autenticación y autorización.
 * <p>
 * Coordina registro, login, JWT y MFA.
 */
@Service
@Transactional
public class AuthService {

    private final UsuarioService usuarioService;
    private final JwtService jwtService;
    private final MfaService mfaService;

    public AuthService(UsuarioService usuarioService, JwtService jwtService, MfaService mfaService) {
        this.usuarioService = usuarioService;
        this.jwtService = jwtService;
        this.mfaService = mfaService;
    }

    /**
     * Registra un nuevo usuario y retorna tokens JWT.
     */
    public AuthResponse registrar(RegisterRequest request) {
        Usuario usuario = usuarioService.registrar(request);

        String role = usuario.getRoles().stream()
                .findFirst()
                .map(r -> "ROLE_" + r.getNombre())
                .orElse("ROLE_ESTUDIANTE");

        String accessToken = jwtService.generateAccessToken(usuario.getEmail(), usuario.getId(), role);
        String refreshToken = jwtService.generateRefreshToken(usuario.getEmail(), usuario.getId());

        return new AuthResponse(
                usuario.getId(),
                usuario.getEmail(),
                usuario.getNombreCompleto(),
                usuario.getRoles().stream().map(r -> "ROLE_" + r.getNombre()).collect(Collectors.toSet()),
                accessToken,
                refreshToken,
                900000L,
                false, // MFA no aplica en registro
                !usuario.isConsentimientoOtorgado()
        );
    }

    /**
     * Autentica un usuario y retorna tokens JWT.
     * Si el usuario tiene MFA habilitado, retorna un token temporal (MFA_REQUIRED)
     * en lugar del access token real.
     */
    public AuthResponse autenticar(LoginRequest request) {
        Usuario usuario = usuarioService.autenticar(request.email(), request.password())
                .orElseThrow(() -> new IllegalArgumentException("Credenciales inválidas o cuenta bloqueada"));

        Set<String> roles = usuario.getRoles().stream()
                .map(r -> "ROLE_" + r.getNombre())
                .collect(Collectors.toSet());

        String role = roles.stream().findFirst().orElse("ROLE_ESTUDIANTE");

        if (usuario.isMfaHabilitado()) {
            // Generar token temporal para MFA
            String mfaToken = jwtService.generateMfaToken(usuario.getEmail(), usuario.getId());
            return new AuthResponse(
                    usuario.getId(),
                    usuario.getEmail(),
                    usuario.getNombreCompleto(),
                    roles,
                    mfaToken,     // Token temporal en lugar de access token
                    null,         // Sin refresh token hasta completar MFA
                    300000L,      // 5 minutos de expiración
                    true,         // requiresMfa = true
                    !usuario.isConsentimientoOtorgado()
            );
        }

        String accessToken = jwtService.generateAccessToken(usuario.getEmail(), usuario.getId(), role);
        String refreshToken = jwtService.generateRefreshToken(usuario.getEmail(), usuario.getId());

        return new AuthResponse(
                usuario.getId(),
                usuario.getEmail(),
                usuario.getNombreCompleto(),
                roles,
                accessToken,
                refreshToken,
                900000L,
                false, // requiresMfa = false porque no tiene MFA habilitado
                !usuario.isConsentimientoOtorgado()
        );
    }

    /**
     * Verifica el código MFA TOTP y retorna los tokens JWT reales.
     */
    public AuthResponse verificarMfa(String tokenTemporal, int codigo) {
        // Validar que el token temporal sea de tipo MFA_REQUIRED
        String type = jwtService.extractClaim(tokenTemporal, claims -> claims.get("type", String.class));
        if (!"mfa_required".equals(type)) {
            throw new IllegalArgumentException("Token temporal inválido");
        }

        String email = jwtService.extractUsername(tokenTemporal);
        UUID userId = jwtService.extractUserId(tokenTemporal);

        if (email == null || userId == null || jwtService.isTokenExpired(tokenTemporal)) {
            throw new IllegalArgumentException("Token temporal expirado o inválido");
        }

        Usuario usuario = usuarioService.buscarPorEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        // Verificar código TOTP
        if (!mfaService.verificarCodigo(usuario.getMfaSecreto(), codigo)) {
            throw new IllegalArgumentException("Código MFA inválido");
        }

        Set<String> roles = usuario.getRoles().stream()
                .map(r -> "ROLE_" + r.getNombre())
                .collect(Collectors.toSet());

        String role = roles.stream().findFirst().orElse("ROLE_ESTUDIANTE");

        // Generar tokens reales
        String accessToken = jwtService.generateAccessToken(usuario.getEmail(), usuario.getId(), role);
        String refreshToken = jwtService.generateRefreshToken(usuario.getEmail(), usuario.getId());

        return new AuthResponse(
                usuario.getId(),
                usuario.getEmail(),
                usuario.getNombreCompleto(),
                roles,
                accessToken,
                refreshToken,
                900000L,
                true,  // requiresMfa = true (confirmado)
                !usuario.isConsentimientoOtorgado()
        );
    }

    /**
     * Refresca el access token usando el refresh token.
     */
    public AuthResponse refreshToken(String refreshToken) {
        String email = jwtService.extractUsername(refreshToken);
        String type = jwtService.extractClaim(refreshToken, claims -> claims.get("type", String.class));

        if (!"refresh".equals(type)) {
            throw new IllegalArgumentException("Token inválido: no es un refresh token");
        }

        Usuario usuario = usuarioService.buscarPorEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        String tokenEmail = jwtService.extractUsername(refreshToken);
        if (!tokenEmail.equals(usuario.getEmail()) || jwtService.isTokenExpired(refreshToken)) {
            throw new IllegalArgumentException("Refresh token inválido o expirado");
        }

        Set<String> roles = usuario.getRoles().stream()
                .map(r -> "ROLE_" + r.getNombre())
                .collect(Collectors.toSet());

        String role = roles.stream().findFirst().orElse("ROLE_ESTUDIANTE");

        String newAccessToken = jwtService.generateAccessToken(usuario.getEmail(), usuario.getId(), role);
        String newRefreshToken = jwtService.generateRefreshToken(usuario.getEmail(), usuario.getId());

        return new AuthResponse(
                usuario.getId(),
                usuario.getEmail(),
                usuario.getNombreCompleto(),
                roles,
                newAccessToken,
                newRefreshToken,
                900000L,
                usuario.isMfaHabilitado(),
                !usuario.isConsentimientoOtorgado()
        );
    }
}
