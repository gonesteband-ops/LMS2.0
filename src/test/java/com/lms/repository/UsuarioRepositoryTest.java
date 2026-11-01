package com.lms.repository;

import com.lms.config.TestConfig;
import com.lms.model.Usuario;
import com.lms.util.HashUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests de integración para UsuarioRepository.
 */
@DataJpaTest
@ActiveProfiles("test")
@Import(TestConfig.class)
@DisplayName("UsuarioRepository - Database Integration")
class UsuarioRepositoryTest {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Test
    @DisplayName("findByEmailAndDeletedAtIsNull: debe encontrar usuario activo")
    void findByEmailAndDeletedAtIsNull() {
        Usuario usuario = Usuario.builder()
                .email("test@example.com")
                .nombreCompleto("Test User")
                .hashContrasena("encoded-password")
                .hashSha256(HashUtils.sha256("test@example.com|Test User"))
                .build();

        usuarioRepository.save(usuario);

        Optional<Usuario> encontrado = usuarioRepository.findByEmailAndDeletedAtIsNull("test@example.com");

        assertTrue(encontrado.isPresent());
        assertEquals("test@example.com", encontrado.get().getEmail());
    }

    @Test
    @DisplayName("existsByEmail: debe detectar email existente")
    void existsByEmail() {
        Usuario usuario = Usuario.builder()
                .email("existente@example.com")
                .nombreCompleto("Existente")
                .hashContrasena("encoded-password")
                .hashSha256(HashUtils.sha256("existente@example.com|Existente"))
                .build();

        usuarioRepository.save(usuario);

        assertTrue(usuarioRepository.existsByEmail("existente@example.com"));
        assertFalse(usuarioRepository.existsByEmail("noexistente@example.com"));
    }
}
