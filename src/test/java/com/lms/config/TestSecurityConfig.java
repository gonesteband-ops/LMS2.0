package com.lms.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configuración de seguridad para tests de integración con @WebMvcTest.
 * <p>
 * NO usa @EnableWebSecurity porque activaría SecurityAutoConfiguration
 * que descubre @Component beans (JwtAuthenticationFilter, etc.) que no
 * están disponibles en el contexto reducido de @WebMvcTest.
 * <p>
 * El @Bean SecurityFilterChain es suficiente para que Spring Security
 * use esta configuración en lugar de la predeterminada.
 * <p>
 * Replica las reglas de seguridad relevantes para los endpoints bajo prueba:
 * - CSRF deshabilitado
 * - Endpoints públicos de API permitidos sin autenticación
 * - GET de cursos público
 * - Demás rutas requieren autenticación
 */
@TestConfiguration
@EnableMethodSecurity
public class TestSecurityConfig {

    @Bean
    public SecurityFilterChain testFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Rutas públicas de API
                .requestMatchers("/api/v1/auth/**").permitAll()
                // GET de cursos es público
                .requestMatchers(HttpMethod.GET, "/api/v1/cursos/**").permitAll()
                // Cualquier otra ruta requiere autenticación
                .anyRequest().authenticated()
            );
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(4);
    }
}
