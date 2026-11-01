package com.lms;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Test de contexto Spring Boot.
 * Verifica que la aplicación cargue correctamente.
 */
@SpringBootTest
@ActiveProfiles("test")
class LMSApplicationTests {

    @Test
    void contextLoads() {
        // Verifica que el contexto de Spring se inicialice sin errores
    }
}
