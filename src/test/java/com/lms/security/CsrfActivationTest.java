package com.lms.security;

import com.lms.config.TestSecurityConfigWithCsrf;
import com.lms.controller.HomeController;
import com.lms.service.ConsentimientoService;
import com.lms.service.InscripcionService;
import com.lms.service.JwtService;
import com.lms.service.RolService;
import com.lms.service.ScormService;
import com.lms.service.UsuarioService;
import com.lms.util.SecurityUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(HomeController.class)
@Import(TestSecurityConfigWithCsrf.class)
@DisplayName("CSRF - Meta tags en templates")
class CsrfActivationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean private JwtService jwtService;
    @MockBean private UsuarioService usuarioService;
    @MockBean private SecurityUtils securityUtils;
    @MockBean private ConsentimientoService consentimientoService;
    @MockBean private RolService rolService;
    @MockBean private InscripcionService inscripcionService;
    @MockBean private ScormService scormService;

    @Test
    @DisplayName("[T] Pagina login incluye meta CSRF")
    void loginPageContieneMetaCsrf() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<meta name=\"_csrf\"")))
                .andExpect(content().string(containsString("<meta name=\"_csrf_header\"")));
    }

    @Test
    @DisplayName("[T] Pagina registro incluye meta CSRF")
    void registroPageContieneMetaCsrf() throws Exception {
        mockMvc.perform(get("/registro"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<meta name=\"_csrf\"")))
                .andExpect(content().string(containsString("<meta name=\"_csrf_header\"")));
    }
}
