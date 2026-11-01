package com.lms.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lms.config.TestSecurityConfig;
import com.lms.dto.AuthResponse;
import com.lms.dto.LoginRequest;
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

import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import(TestSecurityConfig.class)
@DisplayName("AuthController - REST Authentication Endpoints")
class AuthControllerTest {

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
    @DisplayName("POST /api/v1/auth/register - debe registrar usuario exitosamente")
    void registerSuccess() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "Juan P\u00e9rez", "juan@test.com", "Password123!", "Password123!",
                null
        );

        AuthResponse response = new AuthResponse(
                UUID.randomUUID(), "juan@test.com", "Juan P\u00e9rez",
                Set.of("ESTUDIANTE"), "access-token", "refresh-token",
                900000L, false, true
        );

        when(authService.registrar(any(RegisterRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("juan@test.com"))
                .andExpect(jsonPath("$.nombreCompleto").value("Juan P\u00e9rez"))
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.requiresMfa").value(false));
    }

    @Test
    @DisplayName("POST /api/v1/auth/register - debe retornar 400 si el email ya existe")
    void registerEmailDuplicado() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "Juan P\u00e9rez", "existente@test.com", "Password123!", "Password123!",
                null
        );

        when(authService.registrar(any(RegisterRequest.class)))
                .thenThrow(new IllegalArgumentException("El email ya est\u00e1 registrado"));

        mockMvc.perform(post("/api/v1/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/auth/login - debe autenticar y retornar tokens")
    void loginSuccess() throws Exception {
        LoginRequest request = new LoginRequest("juan@test.com", "Password123!");

        AuthResponse response = new AuthResponse(
                UUID.randomUUID(), "juan@test.com", "Juan P\u00e9rez",
                Set.of("ESTUDIANTE"), "access-token-jwt", "refresh-token-jwt",
                900000L, false, false
        );

        when(authService.autenticar(any(LoginRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token-jwt"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token-jwt"))
                .andExpect(jsonPath("$.requiresMfa").value(false))
                .andExpect(header().exists("Set-Cookie"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login - debe retornar 400 si credenciales inv\u00e1lidas")
    void loginInvalidCredentials() throws Exception {
        LoginRequest request = new LoginRequest("juan@test.com", "wrong-password");

        when(authService.autenticar(any(LoginRequest.class)))
                .thenThrow(new IllegalArgumentException("Credenciales inv\u00e1lidas"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
