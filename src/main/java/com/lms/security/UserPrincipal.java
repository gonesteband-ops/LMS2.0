package com.lms.security;

import com.lms.model.Usuario;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.Collection;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Principal personalizado que extiende User de Spring Security
 * e incluye el UUID del usuario para usar en los controladores.
 */
public class UserPrincipal extends User {

    private final UUID userId;
    private final String nombreCompleto;

    public UserPrincipal(Usuario usuario) {
        super(
                usuario.getEmail(),
                usuario.getHashContrasena(),
                usuario.getDeletedAt() == null,
                true, true, true,
                getAuthorities(usuario)
        );
        this.userId = usuario.getId();
        this.nombreCompleto = usuario.getNombreCompleto();
    }

    public UUID getUserId() {
        return userId;
    }

    public String getNombreCompleto() {
        return nombreCompleto;
    }

    private static Collection<? extends GrantedAuthority> getAuthorities(Usuario usuario) {
        // Grant ROLE_* authorities from user's roles
        Collection<GrantedAuthority> authorities = usuario.getRoles().stream()
                .map(rol -> new SimpleGrantedAuthority("ROLE_" + rol.getNombre()))
                .collect(Collectors.toSet());

        // Grant granular permissions as authorities
        authorities.addAll(usuario.getRoles().stream()
                .flatMap(rol -> rol.getPermisos().stream()
                        .map(permiso -> new SimpleGrantedAuthority(
                                permiso.getRecurso() + "_" + permiso.getAccion())))
                .collect(Collectors.toSet()));

        return authorities;
    }
}
