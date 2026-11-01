package com.lms.service;

import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorConfig;
import com.warrenstrange.googleauth.KeyRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Servicio MFA TOTP (Time-based One-Time Password).
 * <p>
 * Implementa autenticación de dos factores usando Google Authenticator
 * o cualquier aplicación compatible con TOTP (RFC 6238).
 */
@Service
public class MfaService {

    private final GoogleAuthenticator gAuth;
    private final String issuer;

    public MfaService(@Value("${app.mfa.issuer:LMS-LegalTech}") String issuer) {
        this.issuer = issuer;

        GoogleAuthenticatorConfig config = new GoogleAuthenticatorConfig.GoogleAuthenticatorConfigBuilder()
                .setKeyRepresentation(KeyRepresentation.BASE32)
                .setCodeDigits(6)
                .setTimeStepSizeInMillis(30000) // 30 segundos
                .setWindowSize(3)               // ±1 ventana de tolerancia (90s)
                .build();

        this.gAuth = new GoogleAuthenticator(config);
    }

    /**
     * Genera un secreto TOTP de 32 caracteres en Base32.
     */
    public String generarSecreto() {
        return gAuth.createCredentials().getKey();
    }

    /**
     * Genera la URL para el código QR (otpauth://).
     * Escanea con Google Authenticator, Authy, etc.
     */
    public String generarQRUrl(String secreto, String email) {
        return String.format("otpauth://totp/%s:%s?secret=%s&issuer=%s&algorithm=SHA1&digits=6&period=30",
                URLEncoder.encode(issuer, StandardCharsets.UTF_8),
                URLEncoder.encode(email, StandardCharsets.UTF_8),
                secreto,
                URLEncoder.encode(issuer, StandardCharsets.UTF_8));
    }

    /**
     * Verifica un código TOTP de 6 dígitos contra el secreto almacenado.
     *
     * @param secreto Secreto Base32 del usuario
     * @param codigo  Código de 6 dígitos ingresado por el usuario
     * @return true si el código es válido
     */
    public boolean verificarCodigo(String secreto, int codigo) {
        if (secreto == null || secreto.isBlank()) {
            return false;
        }
        return gAuth.authorize(secreto, codigo);
    }
}
