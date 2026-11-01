package com.lms.config;

import com.lms.model.Rol;
import com.lms.model.Usuario;
import com.lms.repository.RolRepository;
import com.lms.repository.UsuarioRepository;
import com.lms.util.AuditLogger;
import com.lms.util.HashUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Inicializador de datos semilla para el entorno de desarrollo.
 * <p>
 * Crea usuarios por defecto si no existen en la base de datos.
 * Solo se ejecuta una vez al iniciar la aplicación.
 * <p>
 * Cumplimiento: Los hash SHA-256 garantizan integridad de datos.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogger auditLogger;

    public DataInitializer(UsuarioRepository usuarioRepository,
                           RolRepository rolRepository,
                           PasswordEncoder passwordEncoder,
                           AuditLogger auditLogger) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditLogger = auditLogger;
    }

    @Override
    @Transactional
    public void run(String... args) {
        log.info("Inicializando usuarios por defecto...");
        crearUsuariosSemilla();
        log.info("Usuarios por defecto creados exitosamente.");
    }

    private void crearUsuariosSemilla() {
        List<SeedUser> usuarios = List.of(
                new SeedUser("admin@lms.cl", "Administrador General",
                        variableEntorno("SEED_ADMIN_PASSWORD", "Demo1234!"),
                        "SUPERADMIN", true, "Superadministrador del sistema LMS LegalTech"),
                new SeedUser("instructor@lms.cl", "Instructor Principal",
                        variableEntorno("SEED_INSTRUCTOR_PASSWORD", "Demo1234!"),
                        "INSTRUCTOR", true, "Instructor de cursos de capacitación"),
                new SeedUser("estudiante@lms.cl", "Estudiante Demo",
                        variableEntorno("SEED_ESTUDIANTE_PASSWORD", "Demo1234!"),
                        "ESTUDIANTE", true, "Estudiante del sistema LMS LegalTech")
        );

        for (SeedUser seed : usuarios) {
            var usuarioOpt = usuarioRepository.findByEmailAndDeletedAtIsNull(seed.email());

            if (usuarioOpt.isPresent()) {
                Usuario usuario = usuarioOpt.get();
                // Verificar si el rol del usuario existente coincide con el esperado
                Rol rolEsperado = rolRepository.findByNombre(seed.rolNombre())
                        .orElseThrow(() -> new RuntimeException("Rol " + seed.rolNombre() + " no encontrado."));

                boolean tieneRolCorrecto = usuario.getRoles().stream()
                        .anyMatch(r -> r.getNombre().equals(seed.rolNombre()));

                if (!tieneRolCorrecto) {
                    usuario.getRoles().add(rolEsperado);
                    usuarioRepository.save(usuario);
                    log.info("  ✓ Rol agregado: {} -> {}", seed.email(), seed.rolNombre());
                } else {
                    log.info("  - Usuario ya existe: {} - Omitiendo.", seed.email());
                }
                continue;
            }

            Rol rol = rolRepository.findByNombre(seed.rolNombre())
                    .orElseThrow(() -> new RuntimeException(
                            "Rol " + seed.rolNombre() + " no encontrado. " +
                                    "Ejecute primero la migración V2."));

            String hashContrasena = passwordEncoder.encode(seed.password());
            String hashSha256 = HashUtils.sha256(seed.email() + "|" + seed.nombreCompleto());

            Usuario usuario = Usuario.builder()
                    .email(seed.email().toLowerCase().trim())
                    .nombreCompleto(seed.nombreCompleto().trim())
                    .hashContrasena(hashContrasena)
                    .hashSha256(hashSha256)
                    .intentosFallidos(0)
                    .consentimientoOtorgado(seed.consentimientoOtorgado())
                    .roles(Set.of(rol))
                    .build();

            usuario = usuarioRepository.save(usuario);

            auditLogger.registrarEvento(
                    "USUARIO_SEMILLA_CREADO",
                    usuario.getId(),
                    "INIT",
                    "USUARIO",
                    usuario.getId(),
                    Map.of(
                            "email", usuario.getEmail(),
                            "rol", seed.rolNombre(),
                            "descripcion", seed.descripcion()
                    )
            );

            log.info("  ✓ Usuario creado: {} ({}) - Rol: {}",
                    seed.email(), seed.nombreCompleto(), seed.rolNombre());
        }
    }

    /**
     * Obtiene el valor de una variable de entorno, con un valor por defecto
     * pensado únicamente para entornos de desarrollo y demostración.
     */
    private static String variableEntorno(String clave, String valorPorDefecto) {
        String valor = System.getenv(clave);
        return (valor == null || valor.isBlank()) ? valorPorDefecto : valor;
    }

    /**
     * Registro interno para datos de usuario semilla.
     */
    private record SeedUser(
            String email,
            String nombreCompleto,
            String password,
            String rolNombre,
            boolean consentimientoOtorgado,
            String descripcion
    ) {}
}
