package com.lms.security;

import com.lms.model.Usuario;
import com.lms.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import com.lms.service.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.Collection;

/**
 * Manejador de éxito para login vía formulario.
 * <p>
 * Como el sistema es stateless (JWT), este handler genera un token JWT
 * tras la autenticación exitosa y lo establece como cookie HttpOnly
 * para que el {@link JwtAuthenticationFilter} lo reconozca en requests
 * subsiguientes.
 */
@Component
public class FormLoginSuccessHandler implements AuthenticationSuccessHandler {

    private final JwtService jwtService;
    private final UsuarioRepository usuarioRepository;

    @Value("${app.cookie.secure:false}")
    private boolean cookieSecure;

    public FormLoginSuccessHandler(JwtService jwtService,
                                   UsuarioRepository usuarioRepository) {
        this.jwtService = jwtService;
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException {
        String email = authentication.getName();

        // Buscar el UUID del usuario en BD (requerido por JwtService.generateAccessToken)
        java.util.UUID userId = usuarioRepository.findByEmailAndDeletedAtIsNull(email)
                .map(Usuario::getId)
                .orElse(null);

        // Extraer el primer ROL (no permiso) como String (ej: "ROLE_SUPERADMIN")
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        String role = authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .filter(auth -> auth.startsWith("ROLE_"))
                .findFirst()
                .orElse("ROLE_ESTUDIANTE");
        // Generar JWT (15 minutos de expiración)
        long accessTokenExpiration = 900_000L; // 15 min
        String accessToken = jwtService.generateAccessToken(email, userId, role);

        // Construir cookie HttpOnly con el token usando ResponseCookie (API Spring)
        ResponseCookie responseCookie = ResponseCookie.from("access_token", accessToken)
                .httpOnly(true)
                .secure(this.cookieSecure) // false en desarrollo (HTTP); cambiar a true en producción (HTTPS)
                .path("/")
                .maxAge(accessTokenExpiration / 1000)
                .sameSite("Lax")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, responseCookie.toString());

        // Redirigir al dashboard
        response.sendRedirect("/dashboard");
    }
}
