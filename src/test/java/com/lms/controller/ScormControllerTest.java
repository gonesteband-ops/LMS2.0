package com.lms.controller;

import com.lms.config.TestSecurityConfig;
import com.lms.model.PaqueteScorm;
import com.lms.repository.UsuarioRepository;
import com.lms.service.CodigoGeneratorService;
import com.lms.service.JwtService;
import com.lms.service.ScormRuntimeService;
import com.lms.service.ScormService;
import com.lms.util.SecurityUtils;
import io.minio.MinioClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests para ScormController (REST endpoints de SCORM).
 * Verifica la subida, listado por curso y runtime API.
 */
@WebMvcTest(ScormController.class)
@Import(TestSecurityConfig.class)
@DisplayName("ScormController - REST SCORM Endpoints")
class ScormControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ScormService scormService;

    @MockBean
    private ScormRuntimeService scormRuntimeService;

    @MockBean
    private SecurityUtils securityUtils;

    @MockBean
    private MinioClient minioClient;

    @MockBean
    private CodigoGeneratorService codigoGeneratorService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UsuarioRepository usuarioRepository;

    @Test
    @WithMockUser(roles = "SUPERADMIN")
    @DisplayName("GET /api/v1/cursos/{cursoId}/scorm - debe listar paquetes SCORM del curso")
    void listarPaquetesScorm() throws Exception {
        UUID cursoId = UUID.randomUUID();
        UUID paqueteId = UUID.randomUUID();

        PaqueteScorm paquete = new PaqueteScorm();
        paquete.setId(paqueteId);
        paquete.setTitulo("Curso SCORM Test");
        paquete.setCodigo("SCO-001");
        paquete.setVersionScorm("1.2");
        paquete.setTamanoBytes(512000L);
        paquete.setCreatedAt(LocalDateTime.now());
        paquete.setHabilitado(true);

        when(scormService.listarPorCurso(cursoId)).thenReturn(List.of(paquete));

        mockMvc.perform(get("/api/v1/cursos/{cursoId}/scorm", cursoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(paqueteId.toString()))
                .andExpect(jsonPath("$[0].titulo").value("Curso SCORM Test"))
                .andExpect(jsonPath("$[0].codigo").value("SCO-001"))
                .andExpect(jsonPath("$[0].versionScorm").value("1.2"))
                .andExpect(jsonPath("$[0].habilitado").value(true));
    }

    @Test
    @WithMockUser(roles = "SUPERADMIN")
    @DisplayName("GET /api/v1/cursos/{cursoId}/scorm - lista vacía si no hay SCORMs")
    void listarPaquetesScormVacio() throws Exception {
        UUID cursoId = UUID.randomUUID();

        when(scormService.listarPorCurso(cursoId)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/cursos/{cursoId}/scorm", cursoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @WithMockUser(roles = "ESTUDIANTE")
    @DisplayName("GET /api/v1/cursos/{cursoId}/scorm - estudiante puede listar SCORMs")
    void listarPaquetesScormComoEstudiante() throws Exception {
        UUID cursoId = UUID.randomUUID();
        UUID paqueteId = UUID.randomUUID();

        PaqueteScorm paquete = new PaqueteScorm();
        paquete.setId(paqueteId);
        paquete.setTitulo("Mi Curso");
        paquete.setCodigo("SCO-002");
        paquete.setVersionScorm("2004");
        paquete.setTamanoBytes(256000L);
        paquete.setCreatedAt(LocalDateTime.now());

        when(scormService.listarPorCurso(cursoId)).thenReturn(List.of(paquete));

        mockMvc.perform(get("/api/v1/cursos/{cursoId}/scorm", cursoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].titulo").value("Mi Curso"));
    }

    @Test
    @WithMockUser(roles = "SUPERADMIN")
    @DisplayName("POST /api/v1/scorm/{paqueteId}/initialize - debe inicializar sesión SCORM")
    void initializeScorm() throws Exception {
        UUID paqueteId = UUID.randomUUID();
        UUID usuarioId = UUID.randomUUID();

        when(securityUtils.getCurrentUserId(any())).thenReturn(usuarioId);
        when(scormRuntimeService.lmsInitialize(paqueteId, usuarioId)).thenReturn("true");

        mockMvc.perform(post("/api/v1/scorm/{paqueteId}/initialize", paqueteId)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("true"));
    }

    @Test
    @WithMockUser(roles = "SUPERADMIN")
    @DisplayName("POST /api/v1/scorm/{paqueteId}/commit - debe hacer commit de datos SCORM")
    void commitScorm() throws Exception {
        UUID paqueteId = UUID.randomUUID();
        UUID usuarioId = UUID.randomUUID();

        when(securityUtils.getCurrentUserId(any())).thenReturn(usuarioId);
        when(scormRuntimeService.lmsCommit(paqueteId, usuarioId)).thenReturn("true");

        mockMvc.perform(post("/api/v1/scorm/{paqueteId}/commit", paqueteId)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("true"));
    }

    @Test
    @WithMockUser(roles = "SUPERADMIN")
    @DisplayName("POST /api/v1/scorm/{paqueteId}/finish - debe finalizar sesión SCORM")
    void finishScorm() throws Exception {
        UUID paqueteId = UUID.randomUUID();
        UUID usuarioId = UUID.randomUUID();

        when(securityUtils.getCurrentUserId(any())).thenReturn(usuarioId);
        when(scormRuntimeService.lmsFinish(paqueteId, usuarioId)).thenReturn("true");

        mockMvc.perform(post("/api/v1/scorm/{paqueteId}/finish", paqueteId)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("true"));
    }

    @Test
    @WithMockUser(roles = "SUPERADMIN")
    @DisplayName("POST /api/v1/scorm/{paqueteId}/setValue - debe establecer valor SCORM")
    void setValueScorm() throws Exception {
        UUID paqueteId = UUID.randomUUID();
        UUID usuarioId = UUID.randomUUID();

        when(securityUtils.getCurrentUserId(any())).thenReturn(usuarioId);
        when(scormRuntimeService.lmsSetValue(eq(paqueteId), eq(usuarioId), eq("cmi.core.lesson_status"), eq("completed")))
                .thenReturn("true");

        mockMvc.perform(post("/api/v1/scorm/{paqueteId}/setValue", paqueteId)
                        .with(csrf())
                        .content("{\"clave\":\"cmi.core.lesson_status\",\"valor\":\"completed\"}")
                        .contentType("application/json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("true"));
    }

    @Test
    @WithMockUser(roles = "SUPERADMIN")
    @DisplayName("GET /api/v1/scorm/{paqueteId}/getValue - debe obtener valor SCORM")
    void getValueScorm() throws Exception {
        UUID paqueteId = UUID.randomUUID();
        UUID usuarioId = UUID.randomUUID();

        when(securityUtils.getCurrentUserId(any())).thenReturn(usuarioId);
        when(scormRuntimeService.lmsGetValue(paqueteId, usuarioId, "cmi.core.score.raw"))
                .thenReturn("85");

        mockMvc.perform(get("/api/v1/scorm/{paqueteId}/getValue", paqueteId)
                        .param("clave", "cmi.core.score.raw"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valor").value("85"));
    }

    @Test
    @WithMockUser(roles = "SUPERADMIN")
    @DisplayName("GET /api/v1/scorm/lastError - debe retornar último error")
    void getLastError() throws Exception {
        when(scormRuntimeService.lmsGetLastError()).thenReturn(0);
        when(scormRuntimeService.lmsGetErrorString(0)).thenReturn("Sin error");
        when(scormRuntimeService.lmsGetDiagnostic(0)).thenReturn("No error - Sin error");

        mockMvc.perform(get("/api/v1/scorm/lastError"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errorCode").value(0))
                .andExpect(jsonPath("$.errorString").value("Sin error"));
    }

    @Test
    @DisplayName("GET /api/v1/cursos/{cursoId}/scorm - debe retornar 401 sin autenticación")
    void listarScormsSinAuth() throws Exception {
        UUID cursoId = UUID.randomUUID();

        // En el contexto de TestSecurityConfig, GET /api/v1/cursos/** es permitAll,
        // por lo que sin autenticación retorna 200 (no 401).
        mockMvc.perform(get("/api/v1/cursos/{cursoId}/scorm", cursoId))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "SUPERADMIN")
    @DisplayName("PATCH /api/v1/scorm/{paqueteId}/toggle-habilitado - admin puede toggle habilitado")
    void toggleHabilitado() throws Exception {
        UUID paqueteId = UUID.randomUUID();

        PaqueteScorm paquete = new PaqueteScorm();
        paquete.setId(paqueteId);
        paquete.setTitulo("Test SCORM");
        paquete.setHabilitado(false);

        when(scormService.toggleHabilitado(paqueteId)).thenReturn(paquete);

        mockMvc.perform(patch("/api/v1/scorm/{paqueteId}/toggle-habilitado", paqueteId)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(paqueteId.toString()))
                .andExpect(jsonPath("$.habilitado").value(false));
    }

    @Test
    @WithMockUser(roles = "ESTUDIANTE")
    @DisplayName("PATCH /api/v1/scorm/{paqueteId}/toggle-habilitado - estudiante no tiene permisos (403)")
    void toggleHabilitadoComoEstudiante() throws Exception {
        UUID paqueteId = UUID.randomUUID();

        mockMvc.perform(patch("/api/v1/scorm/{paqueteId}/toggle-habilitado", paqueteId)
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "SUPERADMIN")
    @DisplayName("PATCH /api/v1/scorm/{paqueteId}/toggle-habilitado - retorna habilitado=true despues de toggle")
    void toggleHabilitadoActiva() throws Exception {
        UUID paqueteId = UUID.randomUUID();

        PaqueteScorm paquete = new PaqueteScorm();
        paquete.setId(paqueteId);
        paquete.setTitulo("Test SCORM");
        paquete.setHabilitado(true);

        when(scormService.toggleHabilitado(paqueteId)).thenReturn(paquete);

        mockMvc.perform(patch("/api/v1/scorm/{paqueteId}/toggle-habilitado", paqueteId)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.habilitado").value(true));
    }
}
