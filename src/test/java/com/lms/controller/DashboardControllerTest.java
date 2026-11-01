package com.lms.controller;

import com.lms.config.TestSecurityConfig;
import com.lms.repository.CursoRepository;
import com.lms.repository.DocumentoRepository;
import com.lms.repository.EvaluacionRepository;
import com.lms.repository.EventoAuditoriaRepository;
import com.lms.repository.ExportacionRepository;
import com.lms.repository.FirmaElectronicaRepository;
import com.lms.repository.InscripcionRepository;
import com.lms.repository.IntentoEvaluacionRepository;
import com.lms.util.SecurityUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DashboardController.class)
@Import(TestSecurityConfig.class)
@DisplayName("DashboardController - Resumen KPIs")
class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CursoRepository cursoRepository;

    @MockBean
    private DocumentoRepository documentoRepository;

    @MockBean
    private EvaluacionRepository evaluacionRepository;

    @MockBean
    private EventoAuditoriaRepository eventoAuditoriaRepository;

    @MockBean
    private ExportacionRepository exportacionRepository;

    @MockBean
    private FirmaElectronicaRepository firmaRepository;

    @MockBean
    private InscripcionRepository inscripcionRepository;

    @MockBean
    private IntentoEvaluacionRepository intentoEvaluacionRepository;

    @MockBean
    private SecurityUtils securityUtils;

    @Test
    @WithMockUser(roles = "ESTUDIANTE")
    @DisplayName("GET /api/v1/dashboard/resumen debe incluir evaluacionesPendientes")
    void resumenDebeIncluirEvaluacionesPendientes() throws Exception {
        UUID usuarioId = UUID.randomUUID();

        when(securityUtils.getCurrentUserId(any())).thenReturn(usuarioId);
        when(inscripcionRepository.countByUsuarioIdAndDeletedAtIsNull(any())).thenReturn(3L);
        when(documentoRepository.findByUsuarioInscripciones(any())).thenReturn(java.util.List.of());
        when(firmaRepository.countByUsuarioId(any())).thenReturn(2L);
        when(intentoEvaluacionRepository.countByUsuarioIdAndEstadoNotAndDeletedAtIsNull(any(), any())).thenReturn(5L);
        when(exportacionRepository.countBySolicitanteId(any())).thenReturn(1L);

        mockMvc.perform(get("/api/v1/dashboard/resumen")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.evaluacionesPendientes").value(5))
                .andExpect(jsonPath("$.cursosActivos").value(3))
                .andExpect(jsonPath("$.documentos").isNumber())
                .andExpect(jsonPath("$.firmas").value(2))
                .andExpect(jsonPath("$.exportaciones").value(1));
    }

    @Test
    @WithMockUser(roles = "ESTUDIANTE")
    @DisplayName("GET /api/v1/dashboard/resumen retorna estructura completa")
    void resumenRetornaTodasLasClaves() throws Exception {
        UUID usuarioId = UUID.randomUUID();

        when(securityUtils.getCurrentUserId(any())).thenReturn(usuarioId);
        when(inscripcionRepository.countByUsuarioIdAndDeletedAtIsNull(any())).thenReturn(0L);
        when(documentoRepository.findByUsuarioInscripciones(any())).thenReturn(java.util.List.of());
        when(firmaRepository.countByUsuarioId(any())).thenReturn(0L);
        when(intentoEvaluacionRepository.countByUsuarioIdAndEstadoNotAndDeletedAtIsNull(any(), any())).thenReturn(0L);
        when(exportacionRepository.countBySolicitanteId(any())).thenReturn(0L);

        mockMvc.perform(get("/api/v1/dashboard/resumen")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isMap())
                .andExpect(jsonPath("$.evaluacionesPendientes").isNumber())
                .andExpect(jsonPath("$.cursosActivos").isNumber())
                .andExpect(jsonPath("$.documentos").isNumber())
                .andExpect(jsonPath("$.firmas").isNumber())
                .andExpect(jsonPath("$.exportaciones").isNumber());
    }
}
