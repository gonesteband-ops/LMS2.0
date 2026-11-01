package com.lms.controller;

import com.lms.config.TestSecurityConfig;
import com.lms.model.Documento;
import com.lms.repository.UsuarioRepository;
import com.lms.service.DocumentoService;
import com.lms.service.JwtService;
import com.lms.util.SecurityUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests para DocumentoController (REST endpoints de documentos).
 * Verifica que la serialización JSON no tenga referencias circulares (Bug 1)
 * y que los endpoints funcionen correctamente.
 */
@WebMvcTest(DocumentoController.class)
@Import(TestSecurityConfig.class)
@DisplayName("DocumentoController - REST Document Endpoints")
class DocumentoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DocumentoService documentoService;

    @MockBean
    private SecurityUtils securityUtils;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UsuarioRepository usuarioRepository;

    @Test
    @WithMockUser(roles = "SUPERADMIN")
    @DisplayName("GET /api/v1/documentos/curso/{cursoId} - debe listar documentos de un curso")
    void listarDocumentosPorCurso() throws Exception {
        UUID cursoId = UUID.randomUUID();
        UUID docId = UUID.randomUUID();

        Documento doc = Documento.builder()
                .id(docId)
                .nombreOriginal("manual_java.pdf")
                .urlStorage("documentos/" + docId + "/manual_java.pdf")
                .hashSha256("abc123def456")
                .mimeType("application/pdf")
                .tamanoBytes(204800)
                .version(1)
                .codigo("DOC-001")
                .build();
        doc.setHabilitado(true);

        when(documentoService.listarDocumentosPorCurso(cursoId))
                .thenReturn(List.of(doc));

        mockMvc.perform(get("/api/v1/documentos/curso/{cursoId}", cursoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(docId.toString()))
                .andExpect(jsonPath("$[0].nombreOriginal").value("manual_java.pdf"))
                .andExpect(jsonPath("$[0].mimeType").value("application/pdf"))
                .andExpect(jsonPath("$[0].tamanoBytes").value(204800))
                .andExpect(jsonPath("$[0].codigo").value("DOC-001"))
                .andExpect(jsonPath("$[0].habilitado").value(true))
                // Verificar que NO serializa la propiedad 'cursos' (debe tener @JsonIgnore)
                .andExpect(jsonPath("$[0].cursos").doesNotExist());
    }

    @Test
    @WithMockUser(roles = "SUPERADMIN")
    @DisplayName("GET /api/v1/documentos/curso/{cursoId} - debe retornar lista vacía si no hay documentos")
    void listarDocumentosPorCursoVacio() throws Exception {
        UUID cursoId = UUID.randomUUID();

        when(documentoService.listarDocumentosPorCurso(cursoId))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/v1/documentos/curso/{cursoId}", cursoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @WithMockUser(roles = "ESTUDIANTE")
    @DisplayName("GET /api/v1/documentos/curso/{cursoId} - estudiante autenticado puede ver documentos")
    void listarDocumentosPorCursoComoEstudiante() throws Exception {
        UUID cursoId = UUID.randomUUID();
        UUID docId = UUID.randomUUID();

        Documento doc = Documento.builder()
                .id(docId)
                .nombreOriginal("guia_estudio.pdf")
                .urlStorage("documentos/" + docId + "/guia_estudio.pdf")
                .hashSha256("xyz789")
                .mimeType("application/pdf")
                .tamanoBytes(102400)
                .version(1)
                .codigo("DOC-002")
                .build();

        when(documentoService.listarDocumentosPorCurso(cursoId))
                .thenReturn(List.of(doc));

        mockMvc.perform(get("/api/v1/documentos/curso/{cursoId}", cursoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombreOriginal").value("guia_estudio.pdf"));
    }

    @Test
    @DisplayName("GET /api/v1/documentos/curso/{cursoId} - debe retornar 401 sin autenticación")
    void listarDocumentosSinAuth() throws Exception {
        UUID cursoId = UUID.randomUUID();

        mockMvc.perform(get("/api/v1/documentos/curso/{cursoId}", cursoId))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "SUPERADMIN")
    @DisplayName("PATCH /api/v1/documentos/{id}/toggle-habilitado - admin puede toggle habilitado")
    void toggleHabilitado() throws Exception {
        UUID docId = UUID.randomUUID();

        Documento doc = Documento.builder()
                .id(docId)
                .nombreOriginal("test.pdf")
                .build();
        doc.setHabilitado(false);

        when(documentoService.toggleHabilitado(docId)).thenReturn(doc);

        mockMvc.perform(patch("/api/v1/documentos/{id}/toggle-habilitado", docId)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(docId.toString()))
                .andExpect(jsonPath("$.habilitado").value(false));
    }

    @Test
    @WithMockUser(roles = "ESTUDIANTE")
    @DisplayName("PATCH /api/v1/documentos/{id}/toggle-habilitado - estudiante no tiene permisos (403)")
    void toggleHabilitadoComoEstudiante() throws Exception {
        UUID docId = UUID.randomUUID();

        mockMvc.perform(patch("/api/v1/documentos/{id}/toggle-habilitado", docId)
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "SUPERADMIN")
    @DisplayName("PATCH /api/v1/documentos/{id}/toggle-habilitado - retorna habilitado=true despues de toggle")
    void toggleHabilitadoActiva() throws Exception {
        UUID docId = UUID.randomUUID();

        Documento doc = Documento.builder()
                .id(docId)
                .nombreOriginal("test.pdf")
                .build();
        doc.setHabilitado(true); // Después del toggle, queda true

        when(documentoService.toggleHabilitado(docId)).thenReturn(doc);

        mockMvc.perform(patch("/api/v1/documentos/{id}/toggle-habilitado", docId)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.habilitado").value(true));
    }
}
