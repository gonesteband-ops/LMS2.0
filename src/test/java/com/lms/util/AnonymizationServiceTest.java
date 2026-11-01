package com.lms.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AnonymizationService - Anonimización de PII")
class AnonymizationServiceTest {

    private final AnonymizationService service = new AnonymizationService();

    @Test
    @DisplayName("[T] anonymizarEmail: debe ofuscar email correctamente")
    void anonymizarEmail() {
        assertEquals("a***@c***.cl", service.anonymizarEmail("algo@correo.cl"));
        assertEquals("j***@g***.com", service.anonymizarEmail("juan@gmail.com"));
        assertEquals("a***@b***.com", service.anonymizarEmail("a@b.com"));
    }

    @Test
    @DisplayName("[T] anonymizarEmail: debe retornar null si entrada es null")
    void anonymizarEmailNull() {
        assertNull(service.anonymizarEmail(null));
    }

    @Test
    @DisplayName("[T] anonymizarNombre: debe ofuscar nombre completo")
    void anonymizarNombre() {
        assertEquals("J*** P***", service.anonymizarNombre("Juan Pérez"));
        assertEquals("M***", service.anonymizarNombre("María"));
        assertEquals("A*** B*** D***", service.anonymizarNombre("Ana Beatriz DeLgado"));
    }

    @Test
    @DisplayName("[T] anonymizarIP: debe ofuscar los últimos 2 octetos")
    void anonymizarIP() {
        assertEquals("192.168.***.***", service.anonymizarIP("192.168.1.100"));
        assertEquals("10.0.***.***", service.anonymizarIP("10.0.0.50"));
        assertEquals("0.0.***.***", service.anonymizarIP("0.0.0.0"));
    }

    @Test
    @DisplayName("[T] anonymizarRUT: debe ofuscar el dígito verificador y parte del número")
    void anonymizarRUT() {
        assertEquals("12.345.***-*", service.anonymizarRUT("12.345.678-9"));
        assertEquals("1.234.***-*", service.anonymizarRUT("1.234.567-8"));
    }

    @Test
    @DisplayName("[T] anonymizarMetadata: debe ofuscar valores PII en metadata")
    void anonymizarMetadata() {
        Map<String, Object> metadata = Map.of(
                "email", "juan@test.com",
                "nombre", "Juan Pérez",
                "ip", "192.168.1.1",
                "rut", "12.345.678-9",
                "cursoId", "uuid-123",
                "nota", 85
        );

        Map<String, Object> result = service.anonymizarMetadata(metadata);

        assertEquals("j***@t***.com", result.get("email"));
        assertEquals("J*** P***", result.get("nombre"));
        assertEquals("192.168.***.***", result.get("ip"));
        assertEquals("12.345.***-*", result.get("rut"));
        assertEquals("uuid-123", result.get("cursoId"));
        assertEquals(85, result.get("nota"));
    }

    @Test
    @DisplayName("[T] containsPII: debe detectar si metadata contiene datos sensibles")
    void containsPII() {
        assertTrue(service.containsPII(Map.of("email", "juan@test.com")));
        assertTrue(service.containsPII(Map.of("nombre", "Juan Pérez")));
        assertTrue(service.containsPII(Map.of("rut", "12.345.678-9")));
        assertTrue(service.containsPII(Map.of("ip", "192.168.1.1")));
        assertFalse(service.containsPII(Map.of("cursoId", "uuid-123")));
        assertFalse(service.containsPII(Map.of("nota", 85, "completado", true)));
    }
}
