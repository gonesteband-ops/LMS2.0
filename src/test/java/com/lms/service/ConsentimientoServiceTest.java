package com.lms.service;

import com.lms.dto.ConsentimientoRequest;
import com.lms.model.Consentimiento;
import com.lms.model.Consentimiento.Finalidad;
import com.lms.model.Usuario;
import com.lms.repository.ConsentimientoRepository;
import com.lms.util.AuditLogger;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ConsentimientoService - Consentimiento granular (Ley 21.719)")
class ConsentimientoServiceTest {

    @Mock private ConsentimientoRepository consentimientoRepository;
    @Mock private UsuarioService usuarioService;
    @Mock private AuditLogger auditLogger;
    @Mock private HttpServletRequest httpRequest;

    @InjectMocks
    private ConsentimientoService consentimientoService;

    @Captor
    private ArgumentCaptor<Consentimiento> consentimientoCaptor;

    @Test
    @DisplayName("registrarConsentimiento: debe crear un registro por finalidad activa")
    void registrarConsentimiento() {
        UUID usuarioId = UUID.randomUUID();
        Usuario usuario = Usuario.builder().id(usuarioId).consentimientoOtorgado(false).build();

        when(usuarioService.buscarPorId(usuarioId)).thenReturn(Optional.of(usuario));
        when(httpRequest.getHeader("X-Forwarded-For")).thenReturn("192.168.1.10");
        when(httpRequest.getHeader("User-Agent")).thenReturn("Mozilla/5.0");
        when(consentimientoRepository.save(any(Consentimiento.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ConsentimientoRequest request = new ConsentimientoRequest("1.0", "es-CL",
                Map.of("CUENTA", true, "MARKETING", true, "ANALISIS", false, "COMPARTIR_TERCEROS", false));

        List<Consentimiento> resultados = consentimientoService.registrarConsentimiento(usuarioId, request, httpRequest);

        assertEquals(2, resultados.size());
        verify(consentimientoRepository, times(2)).save(consentimientoCaptor.capture());
        List<Consentimiento> saved = consentimientoCaptor.getAllValues();
        assertTrue(saved.stream().anyMatch(c -> c.getFinalidad() == Finalidad.CUENTA));
        assertTrue(saved.stream().anyMatch(c -> c.getFinalidad() == Finalidad.MARKETING));
        assertTrue(saved.stream().allMatch(Consentimiento::isActivo));
    }

    @Test
    @DisplayName("revocarConsentimiento: debe marcar como inactivo")
    void revocarConsentimiento() {
        UUID usuarioId = UUID.randomUUID();
        Consentimiento activo = Consentimiento.builder()
                .id(UUID.randomUUID()).finalidad(Finalidad.MARKETING).activo(true).build();

        when(consentimientoRepository.findByUsuarioIdAndFinalidadAndActivoTrue(usuarioId, Finalidad.MARKETING))
                .thenReturn(List.of(activo));

        consentimientoService.revocarConsentimiento(usuarioId, "MARKETING");

        assertFalse(activo.isActivo());
        assertNotNull(activo.getFechaRevocacion());
        verify(consentimientoRepository, times(1)).save(activo);
    }

    @Test
    @DisplayName("haOtorgadoConsentimiento: debe retornar true si hay registro activo")
    void haOtorgadoConsentimiento() {
        UUID usuarioId = UUID.randomUUID();
        when(consentimientoRepository.findByUsuarioIdAndFinalidadAndActivoTrue(usuarioId, Finalidad.CUENTA))
                .thenReturn(List.of(Consentimiento.builder().id(UUID.randomUUID()).build()));

        assertTrue(consentimientoService.haOtorgadoConsentimiento(usuarioId, "CUENTA"));
    }
}
