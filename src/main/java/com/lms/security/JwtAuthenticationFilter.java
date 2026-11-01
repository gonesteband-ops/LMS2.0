package com.lms.security;

import com.lms.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filtro JWT que intercepta cada request para validar el token.
 * <p>
 * Soporta tokens en header Authorization: Bearer <token>
 * y en cookie "access_token" (para HTMX).
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtService jwtService, @Lazy UserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        final String path = request.getRequestURI();
        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String email;

        // Intentar obtener token del header Authorization
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            jwt = authHeader.substring(7);
            log.debug("[JWT Filter] Token desde header Authorization. Path: {}", path);
        } else {
            // Intentar obtener token de cookie (para HTMX)
            jwt = extractTokenFromCookie(request);
            log.debug("[JWT Filter] Token desde cookie: {}. Path: {}", jwt != null ? "PRESENTE" : "AUSENTE", path);
        }

        if (jwt == null) {
            log.debug("[JWT Filter] No hay token JWT. Path: {}", path);
            filterChain.doFilter(request, response);
            return;
        }

        try {
            email = jwtService.extractUsername(jwt);
            log.debug("[JWT Filter] Email extraído del token: {}. Path: {}", email, path);
            if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(email);
                boolean tokenValido = jwtService.isTokenValid(jwt, userDetails);
                log.debug("[JWT Filter] Token válido: {}. Usuario: {}. Path: {}", tokenValido, email, path);
                if (tokenValido) {
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails, null, userDetails.getAuthorities()
                            );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    log.debug("[JWT Filter] Autenticación establecida para: {}. Path: {}", email, path);
                }
            }
        } catch (Exception e) {
            log.warn("[JWT Filter] Error validando token JWT: {}. Path: {}", e.getMessage(), path);
        }

        filterChain.doFilter(request, response);
    }

    private String extractTokenFromCookie(HttpServletRequest request) {
        jakarta.servlet.http.Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (jakarta.servlet.http.Cookie cookie : cookies) {
                if ("access_token".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}
