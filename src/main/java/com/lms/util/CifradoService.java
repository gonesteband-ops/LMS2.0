package com.lms.util;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Base64;

@Component
public class CifradoService {

    private static final Logger log = LoggerFactory.getLogger(CifradoService.class);
    private static final String AES_GCM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;

    @Value("${app.cifrado.secret-key:}")
    private String secretKeyBase64;

    private SecretKeySpec secretKey;
    private boolean habilitado;

    @PostConstruct
    void init() {
        if (secretKeyBase64 == null || secretKeyBase64.isBlank()) {
            log.warn("CifradoService: app.cifrado.secret-key no configurada. Cifrado deshabilitado.");
            habilitado = false;
            return;
        }
        try {
            byte[] keyBytes = Base64.getDecoder().decode(secretKeyBase64);
            if (keyBytes.length != 16 && keyBytes.length != 24 && keyBytes.length != 32) {
                log.warn("CifradoService: clave debe ser 16, 24 o 32 bytes (Base64). Cifrado deshabilitado.");
                habilitado = false;
                return;
            }
            secretKey = new SecretKeySpec(keyBytes, "AES");
            habilitado = true;
        } catch (Exception e) {
            log.warn("CifradoService: error inicializando clave: {}. Cifrado deshabilitado.", e.getMessage());
            habilitado = false;
        }
    }

    public String encriptar(String textoPlano) {
        if (!habilitado || textoPlano == null) return textoPlano;
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            SecureRandom.getInstanceStrong().nextBytes(iv);
            Cipher cipher = Cipher.getInstance(AES_GCM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            byte[] cifrado = cipher.doFinal(textoPlano.getBytes());
            ByteBuffer bb = ByteBuffer.allocate(iv.length + cifrado.length);
            bb.put(iv);
            bb.put(cifrado);
            return Base64.getEncoder().encodeToString(bb.array());
        } catch (Exception e) {
            throw new RuntimeException("Error encriptando dato", e);
        }
    }

    public String desencriptar(String textoCifrado) {
        if (!habilitado || textoCifrado == null) return textoCifrado;
        try {
            byte[] decoded = Base64.getDecoder().decode(textoCifrado);
            ByteBuffer bb = ByteBuffer.wrap(decoded);
            byte[] iv = new byte[GCM_IV_LENGTH];
            bb.get(iv);
            byte[] cifrado = new byte[bb.remaining()];
            bb.get(cifrado);
            Cipher cipher = Cipher.getInstance(AES_GCM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            return new String(cipher.doFinal(cifrado));
        } catch (Exception e) {
            throw new RuntimeException("Error desencriptando dato", e);
        }
    }

    public boolean isHabilitado() {
        return habilitado;
    }
}
