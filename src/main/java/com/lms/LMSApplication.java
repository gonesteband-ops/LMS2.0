package com.lms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Aplicación principal del Sistema de Gestión de Aprendizaje (LMS) LegalTech.
 * <p>
 * Stack: Java 17 + Spring Boot 3.3 + HTMX + Thymeleaf + CSS Estático
 * Cumplimiento: Leyes 19.799 y 21.719 (Chile)
 */
@SpringBootApplication
@EnableAsync
@EnableScheduling
public class LMSApplication {

    public static void main(String[] args) {
        SpringApplication.run(LMSApplication.class, args);
    }
}
