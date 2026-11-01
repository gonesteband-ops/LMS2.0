package com.lms.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests para HashUtils (SHA-256).
 */
@DisplayName("HashUtils - SHA-256 Hashing")
class HashUtilsTest {

    @Test
    @DisplayName("sha256: debe calcular hash correctamente")
    void sha256String() {
        String input = "test-input";
        String hash = HashUtils.sha256(input);

        assertNotNull(hash);
        assertEquals(64, hash.length()); // SHA-256 produces 64 hex chars
        assertEquals(hash, HashUtils.sha256(input)); // Deterministic
    }

    @Test
    @DisplayName("sha256: inputs diferentes producen hashes diferentes")
    void sha256DifferentInputs() {
        assertNotEquals(HashUtils.sha256("input1"), HashUtils.sha256("input2"));
    }

    @Test
    @DisplayName("sha256 bytes: debe calcular hash de array de bytes")
    void sha256Bytes() {
        byte[] input = "binary-data".getBytes();
        String hash = HashUtils.sha256(input);

        assertNotNull(hash);
        assertEquals(64, hash.length());
    }

    @Test
    @DisplayName("sha256Encadenado: debe incluir hash anterior en el cálculo")
    void sha256Encadenado() {
        String contenido = "nuevo-evento";
        String hashAnterior = HashUtils.sha256("evento-anterior");

        String hashEncadenado = HashUtils.sha256Encadenado(contenido, hashAnterior);
        String hashEsperado = HashUtils.sha256(contenido + hashAnterior);

        assertEquals(hashEsperado, hashEncadenado);
    }

    @Test
    @DisplayName("sha256Encadenado: hash genesis (sin anterior)")
    void sha256EncadenadoGenesis() {
        String contenido = "primer-evento";
        String hash = HashUtils.sha256Encadenado(contenido, null);

        assertEquals(HashUtils.sha256(contenido + "null"), hash);
    }

    @Test
    @DisplayName("verificarHash: debe validar hash correcto")
    void verificarHash() {
        String input = "datos-verificar";
        String hash = HashUtils.sha256(input);

        assertTrue(HashUtils.verificarHash(input, hash));
        assertFalse(HashUtils.verificarHash("otro-input", hash));
    }
}
