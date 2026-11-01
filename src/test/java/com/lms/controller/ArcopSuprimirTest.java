package com.lms.controller;

import com.lms.config.TestSecurityConfig;
import com.lms.service.ArcopService;
import com.lms.util.SecurityUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ArcopController.class)
@Import(TestSecurityConfig.class)
@DisplayName("ARCOP - Supresion con fecha de gracia y cancelacion")
class ArcopSuprimirTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean private ArcopService arcopService;
    @MockBean private SecurityUtils securityUtils;

    @Test
    @DisplayName("DELETE suprimir con HX-Request devuelve HTML con fecha")
    void suprimirConHTMX() throws Exception {
        mockMvc.perform(delete("/api/v1/arcop/suprimir")
                .header("HX-Request", "true"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("30 dias")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Cancelar solicitud")));
    }

    @Test
    @DisplayName("DELETE suprimir sin HX-Request devuelve JSON")
    void suprimirSinHTMX() throws Exception {
        mockMvc.perform(delete("/api/v1/arcop/suprimir"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }
}
