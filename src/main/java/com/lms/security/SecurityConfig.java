package com.lms.security;

import jakarta.servlet.http.Cookie;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

/**
 * Configuración de seguridad Spring Security.
 * <p>
 * Implementa:
 * - JWT stateless para API REST y formulario web
 * - Autenticación vía formulario que genera JWT en cookie HttpOnly
 * - MFA obligatorio para administradores
 * - Protección contra intentos fallidos
 * - Rutas públicas y privadas
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final FormLoginSuccessHandler formLoginSuccessHandler;

    @Value("${app.cookie.secure:false}")
    private boolean cookieSecure;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthFilter,
                          FormLoginSuccessHandler formLoginSuccessHandler) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.formLoginSuccessHandler = formLoginSuccessHandler;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf
                .ignoringRequestMatchers("/hx/admin/**", "/api/v1/**", "/consentimiento", "/login")
                .csrfTokenRepository(new HttpSessionCsrfTokenRepository())
                .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()))
            // Permitir sesiones HTTP para formularios web; JWT mantiene autenticación entre requests
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
            .authorizeHttpRequests(auth -> auth
                // Rutas públicas
                .requestMatchers("/", "/login", "/registro", "/css/**", "/js/**", "/img/**",
                        "/favicon.ico", "/auth/**", "/error",
                        "/privacidad", "/terminos", "/aviso-legal").permitAll()
                // API pública
                .requestMatchers("/api/v1/auth/**").permitAll()
                // Panel de certificaciones (SUPERADMIN/ADMIN/INSTRUCTOR)
                .requestMatchers("/admin/certificaciones", "/admin/certificaciones/**").hasAnyRole("SUPERADMIN", "ADMIN", "INSTRUCTOR")
                // Admin (solo SUPERADMIN)
                .requestMatchers("/admin/**").hasAnyRole("SUPERADMIN")
                // Instructor
                .requestMatchers("/instructor/**").hasAnyRole("SUPERADMIN", "INSTRUCTOR")
                // Creación y edición de cursos (ESTUDIANTE no puede acceder)
                .requestMatchers("/cursos/crear", "/cursos/editar", "/cursos/evaluaciones/crear", "/cursos/participantes").hasAnyRole("SUPERADMIN", "INSTRUCTOR")
                // Exportaciones (solo SUPERADMIN/INSTRUCTOR)
                .requestMatchers("/dashboard/exportaciones").hasAnyRole("SUPERADMIN", "INSTRUCTOR")
                // Cualquier otra ruta requiere autenticación
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .usernameParameter("email")
                .passwordParameter("password")
                .successHandler(formLoginSuccessHandler)
                .permitAll()
            )
            .logout(logout -> logout
                .logoutRequestMatcher(new AntPathRequestMatcher("/logout"))
                .logoutSuccessUrl("/login?logout")
                .addLogoutHandler((request, response, authentication) -> {
                    // Eliminar la cookie access_token al cerrar sesión
                    Cookie cookie = new Cookie("access_token", null);
                    cookie.setMaxAge(0);
                    cookie.setPath("/");
                    cookie.setHttpOnly(true);
                    cookie.setSecure(cookieSecure);
                    response.addCookie(cookie);
                })
                .permitAll()
            )
            .headers(headers -> headers
                .httpStrictTransportSecurity(hsts -> hsts
                    .includeSubDomains(true)
                    .maxAgeInSeconds(31536000))
                // Permitir iframes del mismo origen (necesario para reproductor SCORM)
                .frameOptions(frameOptions -> frameOptions.sameOrigin())
                // Configurar CSP como header HTTP (frame-ancestors en <meta> es ignorado por navegadores)
                .contentSecurityPolicy(csp -> csp
                    .policyDirectives("default-src 'self'; " +
                            "script-src 'self' 'unsafe-inline' 'unsafe-eval' https://unpkg.com; " +
                            "style-src 'self' 'unsafe-inline' https://fonts.googleapis.com; " +
                            "font-src 'self' https://fonts.gstatic.com; " +
                            "img-src 'self' data:; " +
                            "connect-src 'self'; " +
                            "form-action 'self'; " +
                            "frame-ancestors 'self'; " +
                            "base-uri 'self'")
                )
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}
