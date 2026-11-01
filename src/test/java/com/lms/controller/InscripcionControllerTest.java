package com.lms.controller;

import com.lms.config.TestSecurityConfig;
import com.lms.dto.InscripcionMasivaReport;
import com.lms.dto.InscripcionResponse;
import com.lms.repository.UsuarioRepository;
import com.lms.service.InscripcionService;
import com.lms.service.JwtService;
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

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests para InscripcionController (REST endpoints de inscripciones).
 * Verifica el nuevo endpoint de listado de participantes (Bug 3)
 * y la inscripción individual.
 */
@WebMvcTest(InscripcionController.class)
@Import(TestSecurityConfig.class)
@DisplayName("InscripcionController - REST Enrollment Endpoints")
class InscripcionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private InscripcionService inscripcionService;

    @MockBean
    private SecurityUtils securityUtils;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UsuarioRepository usuarioRepository;

    private InscripcionResponse crearResponse(UUID inscripcionId, UUID cursoId, UUID usuarioId,
                                               String nombreUsuario, String email) {
        return new InscripcionResponse(
                inscripcionId,
                cursoId,
                "Curso Java Básico",
                "PROGRAMACION",
                "publicado",
                false,
                usuarioId,
                nombreUsuario,
                email,
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(30),
                false,
                null,
                null,
                30,
                false,
                null,
                null,
                false,
                null,
                null
        );
    }

    @Test
    @WithMockUser(roles = "SUPERADMIN")
    @DisplayName("GET /api/v1/cursos/{cursoId}/participantes - debe listar participantes inscritos")
    void listarParticipantes() throws Exception {
        UUID cursoId = UUID.randomUUID();
        UUID userId1 = UUID.randomUUID();
        UUID userId2 = UUID.randomUUID();
        UUID inscId1 = UUID.randomUUID();
        UUID inscId2 = UUID.randomUUID();

        InscripcionResponse r1 = crearResponse(inscId1, cursoId, userId1,
                "Juan Pérez", "juan@test.com");
        InscripcionResponse r2 = crearResponse(inscId2, cursoId, userId2,
                "María García", "maria@test.com");

        when(inscripcionService.listarParticipantesPorCurso(cursoId))
                .thenReturn(List.of(r1, r2));

        mockMvc.perform(get("/api/v1/cursos/{cursoId}/participantes", cursoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].usuarioNombre").value("Juan Pérez"))
                .andExpect(jsonPath("$[0].usuarioEmail").value("juan@test.com"))
                .andExpect(jsonPath("$[0].cursoTitulo").value("Curso Java Básico"))
                .andExpect(jsonPath("$[1].usuarioNombre").value("María García"))
                .andExpect(jsonPath("$[1].usuarioEmail").value("maria@test.com"));
    }

    @Test
    @WithMockUser(roles = "SUPERADMIN")
    @DisplayName("GET /api/v1/cursos/{cursoId}/participantes - debe retornar lista vacía si no hay inscritos")
    void listarParticipantesVacio() throws Exception {
        UUID cursoId = UUID.randomUUID();

        when(inscripcionService.listarParticipantesPorCurso(cursoId))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/v1/cursos/{cursoId}/participantes", cursoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @WithMockUser(roles = "SUPERADMIN")
    @DisplayName("POST /api/v1/cursos/{cursoId}/inscribir - debe inscribir usuario en curso")
    void inscribirUsuario() throws Exception {
        UUID cursoId = UUID.randomUUID();
        UUID usuarioId = UUID.randomUUID();
        UUID inscripcionId = UUID.randomUUID();

        InscripcionResponse response = crearResponse(inscripcionId, cursoId, usuarioId,
                "Nuevo Usuario", "nuevo@test.com");

        when(securityUtils.getCurrentUserId(any())).thenReturn(usuarioId);
        when(inscripcionService.resolverIdentificador("12345678-9")).thenReturn(usuarioId);
        when(inscripcionService.inscribirUsuario(cursoId, usuarioId))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/cursos/{cursoId}/inscribir", cursoId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usuarioId\":\"12345678-9\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuarioEmail").value("nuevo@test.com"))
                .andExpect(jsonPath("$.cursoTitulo").value("Curso Java Básico"));
    }

    @Test
    @WithMockUser(roles = "ESTUDIANTE")
    @DisplayName("POST /api/v1/cursos/{cursoId}/inscribir - estudiante no tiene permisos (requiere ADMIN/INSTRUCTOR)")
    void inscribirUsuarioComoEstudiante() throws Exception {
        UUID cursoId = UUID.randomUUID();

        mockMvc.perform(post("/api/v1/cursos/{cursoId}/inscribir", cursoId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usuarioId\":\"12345678-9\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/v1/cursos/{cursoId}/participantes - sin autenticación (403 por @PreAuthorize)")
    void listarParticipantesSinAuth() throws Exception {
        UUID cursoId = UUID.randomUUID();

        // Aunque TestSecurityConfig tiene GET /api/v1/cursos/** como permitAll,
        // @EnableMethodSecurity procesa @PreAuthorize("isAuthenticated()")
        // Por tanto un usuario anónimo recibe 403
        mockMvc.perform(get("/api/v1/cursos/{cursoId}/participantes", cursoId))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "SUPERADMIN")
    @DisplayName("POST /api/v1/cursos/{cursoId}/inscribir-masivo - debe retornar reporte detallado")
    void inscribirMasivo() throws Exception {
        UUID cursoId = UUID.randomUUID();
        UUID usuarioId = UUID.randomUUID();
        UUID inscripcionId = UUID.randomUUID();

        InscripcionResponse response = crearResponse(inscripcionId, cursoId, usuarioId,
                "CSV User", "csv@test.com");

        InscripcionMasivaReport report = new InscripcionMasivaReport(
                1, 1, 0, 0, 0,
                List.of(response), List.of(), List.of(), List.of()
        );

        when(inscripcionService.inscribirMasivo(any(UUID.class), any(org.springframework.web.multipart.MultipartFile.class)))
                .thenReturn(report);

        byte[] csvContent = "email\ncsv@test.com\n".getBytes(StandardCharsets.UTF_8);

        mockMvc.perform(multipart("/api/v1/cursos/{cursoId}/inscribir-masivo", cursoId)
                        .file("archivo", csvContent)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalProcesados").value(1))
                .andExpect(jsonPath("$.exitosos").value(1))
                .andExpect(jsonPath("$.noEncontrados").value(0))
                .andExpect(jsonPath("$.yaInscritos").value(0))
                .andExpect(jsonPath("$.inscripciones[0].usuarioEmail").value("csv@test.com"));
    }

    @Test
    @WithMockUser(roles = "ESTUDIANTE")
    @DisplayName("POST /api/v1/cursos/{cursoId}/inscribir-masivo - estudiante no tiene permisos (403)")
    void inscribirMasivoComoEstudiante() throws Exception {
        UUID cursoId = UUID.randomUUID();

        mockMvc.perform(multipart("/api/v1/cursos/{cursoId}/inscribir-masivo", cursoId)
                        .file("archivo", "email\ntest@test.com\n".getBytes(StandardCharsets.UTF_8))
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }
}
