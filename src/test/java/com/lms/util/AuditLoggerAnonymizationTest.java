package com.lms.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AuditLogger - Anonimización de PII en eventos")
class AuditLoggerAnonymizationTest {

    private final AnonymizationService anonymizationService = new AnonymizationService();

    @Test
    @DisplayName("[V] Evento con PII: metadata se anonimiza via AnonymizationService")
    void eventoConPII_MetadataAnonimizada() {
        Map<String, Object> metadataPII = Map.of(
                "email", "juan@test.com",
                "nombre", "Juan Pérez",
                "cursoId", "uuid-123"
        );

        Map<String, Object> metadataAnon = anonymizationService.anonymizarMetadata(metadataPII);
        assertTrue(anonymizationService.containsPII(metadataPII),
                "Debe detectar PII en metadata original");

        assertEquals("j***@t***.com", metadataAnon.get("email"));
        assertEquals("J*** P***", metadataAnon.get("nombre"));
        assertEquals("uuid-123", metadataAnon.get("cursoId"),
                "Datos no PII deben conservarse intactos");
    }

    @Test
    @DisplayName("[V] Evento sin PII: metadata intacta")
    void eventoSinPII_MetadataIntacta() {
        Map<String, Object> metadataSinPII = Map.of(
                "cursoId", "uuid-456",
                "nota", 92,
                "completado", true
        );

        assertFalse(anonymizationService.containsPII(metadataSinPII),
                "Metadata sin PII no debe ser detectada como PII");

        Map<String, Object> metadataAnon = anonymizationService.anonymizarMetadata(metadataSinPII);
        assertEquals("uuid-456", metadataAnon.get("cursoId"));
        assertEquals(92, metadataAnon.get("nota"));
        assertEquals(true, metadataAnon.get("completado"));
    }

    @Test
    @DisplayName("[V] containsPII: detecta todos los campos PII conocidos")
    void containsPIIDetectaTodosLosCampos() {
        assertTrue(anonymizationService.containsPII(Map.of("email", "a@b.com")));
        assertTrue(anonymizationService.containsPII(Map.of("nombre", "algo")));
        assertTrue(anonymizationService.containsPII(Map.of("nombreCompleto", "algo")));
        assertTrue(anonymizationService.containsPII(Map.of("rut", "12.345.678-9")));
        assertTrue(anonymizationService.containsPII(Map.of("ip", "1.2.3.4")));
        assertTrue(anonymizationService.containsPII(Map.of("ipOrigen", "1.2.3.4")));
        assertTrue(anonymizationService.containsPII(Map.of("userAgent", "Mozilla")), "userAgent debe detectarse como PII");
        assertFalse(anonymizationService.containsPII(Map.of("cursoId", "123")));
        assertFalse(anonymizationService.containsPII(Map.of("nota", 85)));
        assertFalse(anonymizationService.containsPII(null));
    }
}
