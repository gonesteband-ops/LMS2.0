package com.lms.controller;

import com.lms.config.TestSecurityConfig;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import(TestSecurityConfig.class)
@DisplayName("Registro - Validacion edad < 16 (P1.1.5)")
class RegistroEdadValidacionTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @MockBean
    private JwtService jwtService;

    @Test
    @DisplayName("[T] POST form-urlencoded con edad < 16 retorna HTML con error")
    void registroConEdadMenor16_RetornaErrorHTML() throws Exception {
        when(authService.registrar(any()))
                .thenThrow(new IllegalArgumentException(
                    "Debes tener al menos 16 años para registrarte sin autorización de un representante legal"));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("nombreCompleto", "Menor Test")
                        .param("email", "menor@test.com")
                        .param("password", "Password123456!")
                        .param("confirmarPassword", "Password123456!")
                        .param("fechaNacimiento", "2011-11-23"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("16")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("años")));
    }

    @Test
    @DisplayName("[T] POST form-urlencoded con edad >= 16 retorna exito")
    void registroConEdadMayor16_RetornaExito() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("nombreCompleto", "Mayor Test")
                        .param("email", "mayor@test.com")
                        .param("password", "Password123456!")
                        .param("confirmarPassword", "Password123456!")
                        .param("fechaNacimiento", "1990-01-15"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML));
    }
}
