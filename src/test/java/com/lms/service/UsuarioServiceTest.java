package com.lms.service;

import com.lms.dto.RegisterRequest;
import com.lms.model.Rol;
import com.lms.model.Usuario;
import com.lms.repository.RolRepository;
import com.lms.repository.UsuarioRepository;
import com.lms.util.AuditLogger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests para UsuarioService.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UsuarioService - User Registration & Authentication")
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private RolRepository rolRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuditLogger auditLogger;

    @InjectMocks
    private UsuarioService usuarioService;

    @Test
    @DisplayName("registrar: debe crear usuario con rol ESTUDIANTE")
    void registrar() {
        // Arrange
        RegisterRequest request = new RegisterRequest(
                "Juan Pérez",
                "juan@example.com",
                "password123456",
                "password123456",
                null
        );

        Rol rolEstudiante = Rol.builder()
                .id(UUID.randomUUID())
                .nombre("ESTUDIANTE")
                .build();

        when(usuarioRepository.existsByEmail("juan@example.com")).thenReturn(false);
        when(rolRepository.findByNombre("ESTUDIANTE")).thenReturn(Optional.of(rolEstudiante));
        when(passwordEncoder.encode("password123456")).thenReturn("encoded-hash");

        Usuario usuarioGuardado = Usuario.builder()
                .id(UUID.randomUUID())
                .email("juan@example.com")
                .nombreCompleto("Juan Pérez")
                .hashContrasena("encoded-hash")
                .fechaNacimiento(null)
                .roles(Set.of(rolEstudiante))
                .build();

        when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuarioGuardado);

        // Act
        Usuario resultado = usuarioService.registrar(request);

        // Assert
        assertNotNull(resultado);
        assertEquals("juan@example.com", resultado.getEmail());
        assertEquals("Juan Pérez", resultado.getNombreCompleto());
        assertEquals(1, resultado.getRoles().size());
    }

    @Test
    @DisplayName("registrar: debe lanzar excepción si email ya existe")
    void registrarEmailDuplicado() {
        RegisterRequest request = new RegisterRequest(
                "Juan Pérez",
                "juan@example.com",
                "password123456",
                "password123456",
                null
        );

        when(usuarioRepository.existsByEmail("juan@example.com")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> usuarioService.registrar(request));
    }

    @Test
    @DisplayName("registrar: debe bloquear si fechaNacimiento indica edad < 16")
    void registrarBloqueaMenorDe16() {
        LocalDate hace15Anios = LocalDate.now().minusYears(15);
        RegisterRequest request = new RegisterRequest(
                "Menor Test", "menor@test.com", "Password123456!", "Password123456!",
                hace15Anios
        );

        when(usuarioRepository.existsByEmail("menor@test.com")).thenReturn(false);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> usuarioService.registrar(request));
        assertTrue(ex.getMessage().contains("16"),
                "Mensaje debe mencionar los 16 años: " + ex.getMessage());

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("registrar: debe permitir si fechaNacimiento indica edad >= 16")
    void registrarPermiteMayorDe16() {
        LocalDate hace16Anios = LocalDate.now().minusYears(16);
        RegisterRequest request = new RegisterRequest(
                "Mayor Test", "mayor@test.com", "Password123456!", "Password123456!",
                hace16Anios
        );

        Rol rolEstudiante = Rol.builder()
                .id(UUID.randomUUID())
                .nombre("ESTUDIANTE")
                .build();

        when(usuarioRepository.existsByEmail("mayor@test.com")).thenReturn(false);
        when(rolRepository.findByNombre("ESTUDIANTE")).thenReturn(Optional.of(rolEstudiante));
        when(passwordEncoder.encode("Password123456!")).thenReturn("encoded-hash");

        Usuario usuarioGuardado = Usuario.builder()
                .id(UUID.randomUUID())
                .email("mayor@test.com")
                .nombreCompleto("Mayor Test")
                .hashContrasena("encoded-hash")
                .fechaNacimiento(hace16Anios)
                .roles(Set.of(rolEstudiante))
                .build();

        when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuarioGuardado);

        Usuario resultado = usuarioService.registrar(request);
        assertNotNull(resultado);
        assertEquals("mayor@test.com", resultado.getEmail());
    }

    @Test
    @DisplayName("autenticar: debe retornar usuario con credenciales válidas")
    void autenticarExitoso() {
        String email = "juan@example.com";
        String password = "password123456";

        Usuario usuario = Usuario.builder()
                .id(UUID.randomUUID())
                .email(email)
                .hashContrasena("encoded-hash")
                .intentosFallidos(0)
                .build();

        when(usuarioRepository.findByEmailAndDeletedAtIsNull(email)).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches(password, "encoded-hash")).thenReturn(true);
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuario);

        Optional<Usuario> resultado = usuarioService.autenticar(email, password);

        assertTrue(resultado.isPresent());
        assertEquals(email, resultado.get().getEmail());
    }

    @Test
    @DisplayName("autenticar: debe retornar empty con credenciales inválidas")
    void autenticarFallido() {
        String email = "juan@example.com";
        String password = "wrong-password";

        Usuario usuario = Usuario.builder()
                .id(UUID.randomUUID())
                .email(email)
                .hashContrasena("encoded-hash")
                .intentosFallidos(0)
                .build();

        when(usuarioRepository.findByEmailAndDeletedAtIsNull(email)).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches(password, "encoded-hash")).thenReturn(false);
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuario);

        Optional<Usuario> resultado = usuarioService.autenticar(email, password);

        assertFalse(resultado.isPresent());
    }
}
