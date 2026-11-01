package com.lms.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lms.config.TestSecurityConfig;
import com.lms.dto.CursoRequest;
import com.lms.model.Curso;
import com.lms.repository.UsuarioRepository;
import com.lms.service.CursoService;
import com.lms.service.InscripcionService;
import com.lms.service.JwtService;
import com.lms.util.SecurityUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CursoController.class)
@Import(TestSecurityConfig.class)
@DisplayName("CursoController - REST Course Endpoints")
class CursoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CursoService cursoService;

    @MockBean
    private SecurityUtils securityUtils;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UsuarioRepository usuarioRepository;

    @MockBean
    private InscripcionService inscripcionService;

    @Test
    @DisplayName("GET /api/v1/cursos - debe listar cursos paginados")
    void listarCursos() throws Exception {
        Curso curso = Curso.builder()
                .id(UUID.randomUUID())
                .titulo("Java B\u00e1sico")
                .descripcion("Curso de Java")
                .versionActual(1)
                .estado("borrador")
                .categoria("PROGRAMACION")
                .build();

        Page<Curso> page = new PageImpl<>(List.of(curso), PageRequest.of(0, 20), 1);
        when(cursoService.listarCursos(any(PageRequest.class))).thenReturn(page);

        mockMvc.perform(get("/api/v1/cursos")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].titulo").value("Java B\u00e1sico"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("GET /api/v1/cursos/{id} - debe retornar curso por ID")
    void obtenerCursoExistente() throws Exception {
        UUID cursoId = UUID.randomUUID();
        Curso curso = Curso.builder()
                .id(cursoId)
                .titulo("Spring Boot")
                .descripcion("Curso de Spring Boot")
                .versionActual(2)
                .estado("publicado")
                .categoria("PROGRAMACION")
                .build();

        when(cursoService.buscarPorId(cursoId)).thenReturn(Optional.of(curso));

        mockMvc.perform(get("/api/v1/cursos/{id}", cursoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titulo").value("Spring Boot"))
                .andExpect(jsonPath("$.versionActual").value(2));
    }

    @Test
    @DisplayName("GET /api/v1/cursos/{id} - debe retornar 404 si no existe")
    void obtenerCursoNoExistente() throws Exception {
        UUID cursoId = UUID.randomUUID();
        when(cursoService.buscarPorId(cursoId)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/cursos/{id}", cursoId))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "SUPERADMIN")
    @DisplayName("POST /api/v1/cursos - debe crear curso (rol ADMIN)")
    void crearCursoComoAdmin() throws Exception {
        UUID autorId = UUID.randomUUID();
        CursoRequest request = new CursoRequest(
                "Nuevo Curso", "Descripción", "PROGRAMACION",
                "Contenido del curso", null, null, false, null, null
        );

        Curso cursoCreado = Curso.builder()
                .id(UUID.randomUUID())
                .titulo("Nuevo Curso")
                .descripcion("Descripci\u00f3n")
                .versionActual(1)
                .estado("borrador")
                .categoria("PROGRAMACION")
                .build();

        when(securityUtils.getCurrentUserId(any())).thenReturn(autorId);
        when(cursoService.crearCurso(any(CursoRequest.class), eq(autorId))).thenReturn(cursoCreado);

        mockMvc.perform(post("/api/v1/cursos")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titulo").value("Nuevo Curso"))
                .andExpect(jsonPath("$.versionActual").value(1));
    }

    @Test
    @WithMockUser(roles = "SUPERADMIN")
    @DisplayName("PUT /api/v1/cursos/{id} - debe actualizar curso (rol ADMIN)")
    void actualizarCursoComoAdmin() throws Exception {
        UUID cursoId = UUID.randomUUID();
        UUID autorId = UUID.randomUUID();

        CursoRequest request = new CursoRequest(
                "Curso Actualizado", "Descripción actualizada", "PROGRAMACION",
                "Contenido v2", "Se agregó módulo avanzado", null, false, null, null
        );

        Curso cursoActualizado = Curso.builder()
                .id(cursoId)
                .titulo("Curso Actualizado")
                .descripcion("Descripci\u00f3n actualizada")
                .versionActual(2)
                .estado("borrador")
                .categoria("PROGRAMACION")
                .build();

        when(securityUtils.getCurrentUserId(any())).thenReturn(autorId);
        when(cursoService.actualizarCurso(eq(cursoId), any(CursoRequest.class), eq(autorId)))
                .thenReturn(cursoActualizado);

        mockMvc.perform(put("/api/v1/cursos/{id}", cursoId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titulo").value("Curso Actualizado"))
                .andExpect(jsonPath("$.versionActual").value(2));
    }

    @Test
    @WithMockUser(roles = "SUPERADMIN")
    @DisplayName("DELETE /api/v1/cursos/{id} - debe eliminar curso (rol SUPERADMIN)")
    void eliminarCursoComoSuperadmin() throws Exception {
        UUID cursoId = UUID.randomUUID();
        UUID eliminadoPor = UUID.randomUUID();

        when(securityUtils.getCurrentUserId(any())).thenReturn(eliminadoPor);

        mockMvc.perform(delete("/api/v1/cursos/{id}", cursoId)
                        .with(csrf())
                        .param("motivo", "Curso descontinuado"))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(roles = "ESTUDIANTE")
    @DisplayName("POST /api/v1/cursos - debe retornar 403 si no tiene permisos (rol ESTUDIANTE)")
    void crearCursoSinPermisos() throws Exception {
        CursoRequest request = new CursoRequest(
                "Nuevo Curso", "Descripción", "PROGRAMACION",
                "Contenido", null, null, false, null, null
        );

        mockMvc.perform(post("/api/v1/cursos")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}
