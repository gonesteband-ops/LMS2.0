package com.lms.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Utilidades de hashing SHA-256 para integridad de datos.
 * <p>
 * Cumplimiento: LMS.md exige hash SHA-256 para todo archivo y registro crítico.
 */
public final class HashUtils {

    private static final String ALGORITHM = "SHA-256";

    private HashUtils() {
        throw new UnsupportedOperationException("Clase utilitaria");
    }

    /**
     * Calcula hash SHA-256 de un string.
     */
    public static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance(ALGORITHM);
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error calculando SHA-256", e);
        }
    }

    /**
     * Calcula hash SHA-256 de un array de bytes.
     */
    public static String sha256(byte[] input) {
        try {
            MessageDigest digest = MessageDigest.getInstance(ALGORITHM);
            byte[] hash = digest.digest(input);
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error calculando SHA-256", e);
        }
    }

    /**
     * Calcula hash encadenado: SHA-256(contenido + hashAnterior)
     */
    public static String sha256Encadenado(String contenido, String hashAnterior) {
        return sha256(contenido + (hashAnterior != null ? hashAnterior : ""));
    }

    /**
     * Verifica que un hash coincida con el input.
     */
    public static boolean verificarHash(String input, String hashEsperado) {
        return sha256(input).equals(hashEsperado);
    }
}
