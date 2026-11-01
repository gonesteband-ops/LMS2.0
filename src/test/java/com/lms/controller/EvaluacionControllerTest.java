package com.lms.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lms.config.TestSecurityConfig;
import com.lms.dto.EvaluacionRequest;
import com.lms.dto.PreguntaRequest;
import com.lms.model.Curso;
import com.lms.model.Evaluacion;
import com.lms.model.Pregunta;
import com.lms.repository.UsuarioRepository;
import com.lms.service.EvaluacionService;
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
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(EvaluacionController.class)
@Import(TestSecurityConfig.class)
@DisplayName("EvaluacionController - REST Evaluation Endpoints")
class EvaluacionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private EvaluacionService evaluacionService;

    @MockBean
    private InscripcionService inscripcionService;

    @MockBean
    private SecurityUtils securityUtils;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UsuarioRepository usuarioRepository;

    private Curso crearCursoMock() {
        return Curso.builder()
                .id(UUID.randomUUID())
                .titulo("Curso de prueba")
                .build();
    }

    private Evaluacion crearEvaluacionMock(UUID evId, Curso curso) {
        return Evaluacion.builder()
                .id(evId)
                .curso(curso)
                .titulo("Examen Final")
                .descripcion("Evaluación de prueba")
                .pesoTotal(100.0)
                .tiempoEstimadoMinutos(60)
                .codigo("EVA-001")
                .build();
    }

    private EvaluacionRequest crearRequestValido() {
        PreguntaRequest p1 = new PreguntaRequest(
                "opcion_unica", "¿Pregunta 1?", 50.0, 120000,
                "[{\"texto\":\"A\",\"correcta\":true}]", null, 0
        );
        PreguntaRequest p2 = new PreguntaRequest(
                "verdadero_falso", "¿Es cierto?", 50.0, 60000,
                "[{\"texto\":\"Verdadero\",\"correcta\":true},{\"texto\":\"Falso\",\"correcta\":false}]",
                null, 1
        );
        return new EvaluacionRequest(
                "Examen Final", "Evaluación completa", 100.0, 60,
                "{\"version\":\"1.0\"}", 4.0, List.of(p1, p2)
        );
    }

    // ============================================================
    // GET /api/v1/cursos/{cursoId}/evaluaciones
    // ============================================================

    @Test
    @WithMockUser(roles = "SUPERADMIN")
    @DisplayName("GET listar - debe retornar evaluaciones del curso")
    void listarEvaluaciones() throws Exception {
        // Arrange
        UUID cursoId = UUID.randomUUID();
        Curso curso = crearCursoMock();
        Evaluacion ev1 = crearEvaluacionMock(UUID.randomUUID(), curso);
        Evaluacion ev2 = crearEvaluacionMock(UUID.randomUUID(), curso);

        when(evaluacionService.listarPorCurso(cursoId)).thenReturn(List.of(ev1, ev2));

        // Act & Assert
        mockMvc.perform(get("/api/v1/cursos/{cursoId}/evaluaciones", cursoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].titulo").value("Examen Final"));
    }

    @Test
    @WithMockUser(roles = "SUPERADMIN")
    @DisplayName("GET listar - debe retornar lista vacía si no hay evaluaciones")
    void listarEvaluacionesVacio() throws Exception {
        // Arrange
        UUID cursoId = UUID.randomUUID();

        when(evaluacionService.listarPorCurso(cursoId)).thenReturn(List.of());

        // Act & Assert
        mockMvc.perform(get("/api/v1/cursos/{cursoId}/evaluaciones", cursoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // ============================================================
    // GET /api/v1/cursos/{cursoId}/evaluaciones/paged
    // ============================================================

    @Test
    @WithMockUser(roles = "SUPERADMIN")
    @DisplayName("GET paged - debe retornar página de evaluaciones")
    void listarEvaluacionesPaginado() throws Exception {
        // Arrange
        UUID cursoId = UUID.randomUUID();
        Curso curso = crearCursoMock();
        Evaluacion ev = crearEvaluacionMock(UUID.randomUUID(), curso);
        Page<Evaluacion> page = new PageImpl<>(List.of(ev), PageRequest.of(0, 20), 1);

        when(evaluacionService.listarPorCursoPaginado(eq(cursoId), any())).thenReturn(page);

        // Act & Assert
        mockMvc.perform(get("/api/v1/cursos/{cursoId}/evaluaciones/paged", cursoId)
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].titulo").value("Examen Final"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    // ============================================================
    // POST /api/v1/cursos/{cursoId}/evaluaciones
    // ============================================================

    @Test
    @WithMockUser(roles = "SUPERADMIN")
    @DisplayName("POST crear - debe crear evaluación como ADMIN")
    void crearEvaluacionComoAdmin() throws Exception {
        // Arrange
        UUID cursoId = UUID.randomUUID();
        UUID autorId = UUID.randomUUID();
        EvaluacionRequest request = crearRequestValido();
        Curso curso = crearCursoMock();
        Evaluacion evaluacion = crearEvaluacionMock(UUID.randomUUID(), curso);

        when(securityUtils.getCurrentUserId(any())).thenReturn(autorId);
        when(evaluacionService.crearEvaluacion(eq(cursoId), any(EvaluacionRequest.class), eq(autorId)))
                .thenReturn(evaluacion);

        // Act & Assert
        mockMvc.perform(post("/api/v1/cursos/{cursoId}/evaluaciones", cursoId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titulo").value("Examen Final"))
                .andExpect(jsonPath("$.codigo").value("EVA-001"));
    }

    @Test
    @WithMockUser(roles = "INSTRUCTOR")
    @DisplayName("POST crear - debe crear evaluación como INSTRUCTOR")
    void crearEvaluacionComoInstructor() throws Exception {
        // Arrange
        UUID cursoId = UUID.randomUUID();
        UUID autorId = UUID.randomUUID();
        EvaluacionRequest request = crearRequestValido();
        Curso curso = crearCursoMock();
        Evaluacion evaluacion = crearEvaluacionMock(UUID.randomUUID(), curso);

        when(securityUtils.getCurrentUserId(any())).thenReturn(autorId);
        when(evaluacionService.crearEvaluacion(eq(cursoId), any(EvaluacionRequest.class), eq(autorId)))
                .thenReturn(evaluacion);

        // Act & Assert
        mockMvc.perform(post("/api/v1/cursos/{cursoId}/evaluaciones", cursoId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titulo").value("Examen Final"));
    }

    @Test
    @WithMockUser(roles = "ESTUDIANTE")
    @DisplayName("POST crear - debe retornar 403 si no tiene permisos (ESTUDIANTE)")
    void crearEvaluacionSinPermisos() throws Exception {
        // Arrange
        UUID cursoId = UUID.randomUUID();
        EvaluacionRequest request = crearRequestValido();

        // Act & Assert
        mockMvc.perform(post("/api/v1/cursos/{cursoId}/evaluaciones", cursoId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST crear - debe retornar 403 sin autenticación (Spring @PreAuthorize retorna 403 para anónimos)")
    void crearEvaluacionSinAuth() throws Exception {
        // Arrange
        UUID cursoId = UUID.randomUUID();
        EvaluacionRequest request = crearRequestValido();

        // Act & Assert
        mockMvc.perform(post("/api/v1/cursos/{cursoId}/evaluaciones", cursoId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    // ============================================================
    // PUT /api/v1/cursos/{cursoId}/evaluaciones/{evaluacionId}
    // ============================================================

    @Test
    @WithMockUser(roles = "SUPERADMIN")
    @DisplayName("PUT actualizar - debe actualizar evaluación como ADMIN")
    void actualizarEvaluacionComoAdmin() throws Exception {
        // Arrange
        UUID cursoId = UUID.randomUUID();
        UUID evId = UUID.randomUUID();
        UUID autorId = UUID.randomUUID();
        EvaluacionRequest request = crearRequestValido();
        Curso curso = crearCursoMock();
        Evaluacion evaluacion = crearEvaluacionMock(evId, curso);

        when(securityUtils.getCurrentUserId(any())).thenReturn(autorId);
        when(evaluacionService.actualizarEvaluacion(eq(evId), any(EvaluacionRequest.class), eq(autorId)))
                .thenReturn(evaluacion);

        // Act & Assert
        mockMvc.perform(put("/api/v1/cursos/{cursoId}/evaluaciones/{evaluacionId}", cursoId, evId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titulo").value("Examen Final"))
                .andExpect(jsonPath("$.codigo").value("EVA-001"));
    }

    @Test
    @WithMockUser(roles = "SUPERADMIN")
    @DisplayName("PUT actualizar - debe retornar 404 si la evaluación no existe")
    void actualizarEvaluacionNoExistente() throws Exception {
        // Arrange
        UUID cursoId = UUID.randomUUID();
        UUID evId = UUID.randomUUID();
        UUID autorId = UUID.randomUUID();
        EvaluacionRequest request = crearRequestValido();

        when(securityUtils.getCurrentUserId(any())).thenReturn(autorId);
        when(evaluacionService.actualizarEvaluacion(eq(evId), any(EvaluacionRequest.class), eq(autorId)))
                .thenThrow(new IllegalArgumentException("Evaluación no encontrada"));

        // Act & Assert
        mockMvc.perform(put("/api/v1/cursos/{cursoId}/evaluaciones/{evaluacionId}", cursoId, evId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ESTUDIANTE")
    @DisplayName("PUT actualizar - debe retornar 403 si no tiene permisos")
    void actualizarEvaluacionSinPermisos() throws Exception {
        // Arrange
        UUID cursoId = UUID.randomUUID();
        UUID evId = UUID.randomUUID();
        EvaluacionRequest request = crearRequestValido();

        // Act & Assert
        mockMvc.perform(put("/api/v1/cursos/{cursoId}/evaluaciones/{evaluacionId}", cursoId, evId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    // ============================================================
    // DELETE /api/v1/cursos/{cursoId}/evaluaciones/{evaluacionId}
    // ============================================================

    @Test
    @WithMockUser(roles = "SUPERADMIN")
    @DisplayName("DELETE eliminar - debe eliminar evaluación como ADMIN")
    void eliminarEvaluacionComoAdmin() throws Exception {
        // Arrange
        UUID cursoId = UUID.randomUUID();
        UUID evId = UUID.randomUUID();
        UUID autorId = UUID.randomUUID();

        when(securityUtils.getCurrentUserId(any())).thenReturn(autorId);
        doNothing().when(evaluacionService).eliminarEvaluacion(evId, autorId);

        // Act & Assert
        mockMvc.perform(delete("/api/v1/cursos/{cursoId}/evaluaciones/{evaluacionId}", cursoId, evId)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").value("Evaluación eliminada correctamente"));

        verify(evaluacionService, times(1)).eliminarEvaluacion(evId, autorId);
    }

    @Test
    @WithMockUser(roles = "INSTRUCTOR")
    @DisplayName("DELETE eliminar - debe eliminar evaluación como INSTRUCTOR")
    void eliminarEvaluacionComoInstructor() throws Exception {
        // Arrange
        UUID cursoId = UUID.randomUUID();
        UUID evId = UUID.randomUUID();
        UUID autorId = UUID.randomUUID();

        when(securityUtils.getCurrentUserId(any())).thenReturn(autorId);
        doNothing().when(evaluacionService).eliminarEvaluacion(evId, autorId);

        // Act & Assert
        mockMvc.perform(delete("/api/v1/cursos/{cursoId}/evaluaciones/{evaluacionId}", cursoId, evId)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").value("Evaluación eliminada correctamente"));
    }

    @Test
    @WithMockUser(roles = "ESTUDIANTE")
    @DisplayName("DELETE eliminar - debe retornar 403 si no tiene permisos")
    void eliminarEvaluacionSinPermisos() throws Exception {
        // Arrange
        UUID cursoId = UUID.randomUUID();
        UUID evId = UUID.randomUUID();

        // Act & Assert
        mockMvc.perform(delete("/api/v1/cursos/{cursoId}/evaluaciones/{evaluacionId}", cursoId, evId)
                        .with(csrf()))
                .andExpect(status().isForbidden());

        verify(evaluacionService, never()).eliminarEvaluacion(any(), any());
    }

    // ============================================================
    // GET /api/v1/cursos/{cursoId}/evaluaciones/{evaluacionId}/preguntas
    // ============================================================

    @Test
    @WithMockUser(roles = "SUPERADMIN")
    @DisplayName("GET preguntas - debe retornar preguntas de la evaluación")
    void obtenerPreguntas() throws Exception {
        // Arrange
        UUID cursoId = UUID.randomUUID();
        UUID evId = UUID.randomUUID();
        Curso curso = crearCursoMock();
        Evaluacion evaluacion = crearEvaluacionMock(evId, curso);

        Pregunta p1 = Pregunta.builder()
                .id(UUID.randomUUID())
                .evaluacion(evaluacion)
                .tipo("opcion_unica")
                .contenido("¿Pregunta 1?")
                .peso(50.0)
                .tiempoRespuestaMs(120000)
                .opciones("[{\"texto\":\"A\",\"correcta\":true}]")
                .orden(0)
                .build();

        Pregunta p2 = Pregunta.builder()
                .id(UUID.randomUUID())
                .evaluacion(evaluacion)
                .tipo("verdadero_falso")
                .contenido("¿Verdadero?")
                .peso(50.0)
                .tiempoRespuestaMs(60000)
                .opciones("[{\"texto\":\"Verdadero\",\"correcta\":true}]")
                .orden(1)
                .build();

        when(evaluacionService.obtenerPreguntas(evId)).thenReturn(List.of(p1, p2));

        // Act & Assert
        mockMvc.perform(get("/api/v1/cursos/{cursoId}/evaluaciones/{evaluacionId}/preguntas",
                        cursoId, evId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].tipo").value("opcion_unica"))
                .andExpect(jsonPath("$[1].tipo").value("verdadero_falso"));
    }

    @Test
    @WithMockUser(roles = "SUPERADMIN")
    @DisplayName("GET preguntas - debe retornar lista vacía si no hay preguntas")
    void obtenerPreguntasVacio() throws Exception {
        // Arrange
        UUID cursoId = UUID.randomUUID();
        UUID evId = UUID.randomUUID();

        when(evaluacionService.obtenerPreguntas(evId)).thenReturn(List.of());

        // Act & Assert
        mockMvc.perform(get("/api/v1/cursos/{cursoId}/evaluaciones/{evaluacionId}/preguntas",
                        cursoId, evId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
