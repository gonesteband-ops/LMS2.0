/*  */package com.lms.util;

import com.lms.model.EventoAuditoria;
import com.lms.repository.EventoAuditoriaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests para AuditLogger (Event Sourcing con hash encadenado).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuditLogger - Event Sourcing with Chained Hashing")
class AuditLoggerTest {

    @Mock
    private EventoAuditoriaRepository repository;

    @InjectMocks
    private AuditLogger auditLogger;

    @Test
    @DisplayName("registrarEvento: debe crear evento con hash encadenado")
    void registrarEvento() {
        // Arrange
        when(repository.findUltimoHash()).thenReturn(Optional.empty());

        // Act
        auditLogger.registrarEvento(
                "TEST_EVENT",
                UUID.randomUUID(),
                "TEST",
                "TEST",
                UUID.randomUUID(),
                Map.of("key", "value")
        );

        // Assert
        verify(repository).save(any(EventoAuditoria.class));
    }
}
