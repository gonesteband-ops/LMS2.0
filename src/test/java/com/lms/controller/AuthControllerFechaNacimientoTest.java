package com.lms.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lms.config.TestSecurityConfig;
import com.lms.dto.AuthResponse;
import com.lms.dto.RegisterRequest;
import com.lms.repository.UsuarioRepository;
import com.lms.service.AuthService;
import com.lms.service.JwtService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import(TestSecurityConfig.class)
@DisplayName("AuthController - fechaNacimiento en registro")
class AuthControllerFechaNacimientoTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UsuarioRepository usuarioRepository;

    @Test
    @DisplayName("POST /api/v1/auth/register con fechaNacimiento - debe retornar 200")
    void registerConFechaNacimiento() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "Juan Pérez", "juan@test.com", "Password123!", "Password123!",
                LocalDate.of(1990, 1, 15)
        );

        AuthResponse response = new AuthResponse(
                UUID.randomUUID(), "juan@test.com", "Juan Pérez",
                Set.of("ESTUDIANTE"), "access-token", "refresh-token",
                900000L, false, true
        );

        when(authService.registrar(any(RegisterRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("juan@test.com"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/register sin fechaNacimiento - debe retornar 200 (opcional)")
    void registerSinFechaNacimiento() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "Juan Pérez", "juan@test.com", "Password123!", "Password123!",
                null
        );

        AuthResponse response = new AuthResponse(
                UUID.randomUUID(), "juan@test.com", "Juan Pérez",
                Set.of("ESTUDIANTE"), "access-token", "refresh-token",
                900000L, false, true
        );

        when(authService.registrar(any(RegisterRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }
}
