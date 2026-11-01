package com.lms.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Controller advice global que expone el request URI actual en todos los modelos Thymeleaf.
 * Necesario porque en Spring Boot 3.3 + Thymeleaf 3.1, {@code #request} no está disponible
 * directamente en los templates.
 */
@ControllerAdvice
public class GlobalControllerAdvice {

    @ModelAttribute("currentRequestUri")
    public String currentRequestUri(HttpServletRequest request) {
        return request.getRequestURI();
    }
}
