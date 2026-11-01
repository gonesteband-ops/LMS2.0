package com.lms.repository;

import com.lms.config.TestConfig;
import com.lms.model.Usuario;
import com.lms.util.HashUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@TestPropertySource(properties = {
    "spring.flyway.enabled=false",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Import(TestConfig.class)
@DisplayName("FechaNacimiento - Validacion de persistencia en BD")
class FechaNacimientoPersistenciaTest {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private EntityManager entityManager;

    private Usuario crearUsuario(String email, String nombre, LocalDate fechaNacimiento) {
        return Usuario.builder()
                .email(email)
                .nombreCompleto(nombre)
                .hashContrasena("encoded-" + UUID.randomUUID())
                .hashSha256(HashUtils.sha256(email + "|" + nombre))
                .fechaNacimiento(fechaNacimiento)
                .build();
    }

    @Test
    @DisplayName("[V] Usuario con fechaNacimiento se persiste en BD")
    void persistenciaFechaNacimiento() {
        LocalDate fechaNacimiento = LocalDate.of(1990, 1, 15);
        String email = "v-persistencia-test@example.com";
        Usuario usuario = crearUsuario(email, "Test Valido", fechaNacimiento);

        Usuario guardado = usuarioRepository.save(usuario);
        entityManager.flush();
        assertNotNull(guardado.getId());

        Optional<Usuario> recuperado = usuarioRepository.findByEmailAndDeletedAtIsNull(email);
        assertTrue(recuperado.isPresent());
        assertEquals(fechaNacimiento, recuperado.get().getFechaNacimiento());

        Object valorBD = entityManager
                .createNativeQuery("SELECT fecha_nacimiento FROM usuarios WHERE id = ?")
                .setParameter(1, guardado.getId())
                .getSingleResult();
        assertEquals(java.sql.Date.valueOf("1990-01-15"), valorBD);
    }

    @Test
    @DisplayName("[V] Usuario sin fechaNacimiento guarda NULL")
    void persistenciaFechaNacimientoNull() {
        String email = "v-persistencia-null@example.com";
        Usuario usuario = crearUsuario(email, "Test Null", null);

        Usuario guardado = usuarioRepository.save(usuario);
        entityManager.flush();
        assertNotNull(guardado.getId());

        Object valorBD = entityManager
                .createNativeQuery("SELECT fecha_nacimiento FROM usuarios WHERE id = ?")
                .setParameter(1, guardado.getId())
                .getSingleResult();

        assertNull(valorBD);
    }
}
