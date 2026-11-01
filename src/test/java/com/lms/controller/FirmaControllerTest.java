package com.lms.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lms.config.TestSecurityConfig;
import com.lms.dto.FirmaRequest;
import com.lms.dto.FirmaResponse;
import com.lms.repository.UsuarioRepository;
import com.lms.service.FirmaService;
import com.lms.service.JwtService;
import com.lms.util.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(FirmaController.class)
@Import(TestSecurityConfig.class)
@DisplayName("FirmaController - REST Electronic Signature Endpoints")
class FirmaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private FirmaService firmaService;

    @MockBean
    private SecurityUtils securityUtils;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UsuarioRepository usuarioRepository;

    @Test
    @WithMockUser
    @DisplayName("POST /api/v1/firmas - debe registrar firma electr\u00f3nica (autenticado)")
    void firmarDocumentoAutenticado() throws Exception {
        UUID usuarioId = UUID.randomUUID();
        UUID documentoId = UUID.randomUUID();

        FirmaRequest request = new FirmaRequest(documentoId, "Windows 11", "CL-SCL");

        FirmaResponse response = new FirmaResponse(
                UUID.randomUUID(),
                usuarioId,
                documentoId,
                "hash-documento",
                "hash-encadenado",
                "hash-anterior",
                LocalDateTime.now()
        );

        when(securityUtils.getCurrentUserId(any())).thenReturn(usuarioId);
        when(firmaService.firmarDocumento(any(UUID.class), any(FirmaRequest.class), any(HttpServletRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/firmas")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuarioId").value(usuarioId.toString()))
                .andExpect(jsonPath("$.documentoId").value(documentoId.toString()))
                .andExpect(jsonPath("$.hashSha256").value("hash-encadenado"))
                .andExpect(jsonPath("$.hashAnterior").value("hash-anterior"));
    }

    @Test
    @DisplayName("POST /api/v1/firmas - debe retornar 401 si no est\u00e1 autenticado")
    void firmarDocumentoSinAutenticar() throws Exception {
        FirmaRequest request = new FirmaRequest(UUID.randomUUID(), "Linux", null);

        mockMvc.perform(post("/api/v1/firmas")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }
}
