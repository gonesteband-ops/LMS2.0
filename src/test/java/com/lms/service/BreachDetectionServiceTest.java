package com.lms.service;

import com.lms.model.IncidenteSeguridad;
import com.lms.repository.IncidenteSeguridadRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BreachDetectionService - Deteccion de ataques")
class BreachDetectionServiceTest {

    @Mock
    private IncidenteSeguridadRepository repository;

    @InjectMocks
    private BreachDetectionService service;

    @Captor
    private ArgumentCaptor<IncidenteSeguridad> incidenteCaptor;

    @Test
    @DisplayName("[T] 50+ intentos fallidos desde misma IP genera incidente")
    void cincuentaIntentosMismaIP() {
        for (int i = 0; i < 51; i++) {
            service.registrarIntentoFallido("test@test.com", "192.168.1.1");
        }

        verify(repository, atLeastOnce()).save(incidenteCaptor.capture());
        IncidenteSeguridad incidente = incidenteCaptor.getValue();
        assertEquals("ATAQUE_FUERZA_BRUTA", incidente.getTipo());
        assertEquals("MEDIO", incidente.getNivelRiesgo());
        assertEquals("DETECTADO", incidente.getEstado());
        assertNotNull(incidente.getHashSha256());
    }

    @Test
    @DisplayName("[T] < 50 intentos NO genera incidente")
    void pocosIntentosNoGeneraIncidente() {
        for (int i = 0; i < 5; i++) {
            service.registrarIntentoFallido("seguro@test.com", "10.0.0.1");
        }

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("[V] Incidente guardado contiene hash SHA-256 valido")
    void incidenteContieneHashValido() {
        for (int i = 0; i < 51; i++) {
            service.registrarIntentoFallido("hash@test.com", "10.0.0.1");
        }

        verify(repository, atLeastOnce()).save(incidenteCaptor.capture());
        IncidenteSeguridad incidente = incidenteCaptor.getValue();
        assertNotNull(incidente.getHashSha256());
        assertEquals(64, incidente.getHashSha256().length());
        assertTrue(incidente.getDescripcion().contains("hash@test.com"));
    }
}
