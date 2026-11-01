package com.lms.controller;

import com.lms.config.TestSecurityConfig;
import com.lms.service.ConsentimientoService;
import com.lms.util.SecurityUtils;
import com.lms.service.UsuarioService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(HomeController.class)
@Import(TestSecurityConfig.class)
@DisplayName("Privacidad - Pagina de privacidad")
class PrivacidadTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean private UsuarioService usuarioService;
    @MockBean private SecurityUtils securityUtils;
    @MockBean private ConsentimientoService consentimientoService;

    @Test
    @DisplayName("GET /dashboard/privacidad carga correctamente")
    void privacidadPageLoads() throws Exception {
        mockMvc.perform(get("/dashboard/privacidad"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Privacidad")));
    }

    @Test
    @DisplayName("GET /consentimiento carga correctamente")
    void consentimientoPageLoads() throws Exception {
        mockMvc.perform(get("/consentimiento"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Finalidades")));
    }
}
