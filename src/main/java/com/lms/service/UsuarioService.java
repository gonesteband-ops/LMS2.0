package com.lms.service;

import com.lms.dto.CargaMasivaUsuariosReport;
import com.lms.dto.CrearUsuarioRequest;
import com.lms.dto.EditarUsuarioRequest;
import com.lms.dto.RegisterRequest;
import com.lms.model.Rol;
import com.lms.model.Usuario;
import com.lms.repository.RolRepository;
import com.lms.repository.UsuarioRepository;
import com.lms.util.AuditLogger;
import com.lms.util.HashUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Servicio de gestión de usuarios.
 * <p>
 * Implementa: registro, bloqueo por intentos, soft-delete, hash SHA-256.
 */
@Service
@Transactional
public class UsuarioService {

    private static final int MAX_INTENTOS_FALLIDOS = 5;
    private static final long BLOQUEO_MINUTOS = 30;

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogger auditLogger;

    public UsuarioService(UsuarioRepository usuarioRepository,
                          RolRepository rolRepository,
                          PasswordEncoder passwordEncoder,
                          AuditLogger auditLogger) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditLogger = auditLogger;
    }

    /**
     * Lista usuarios activos (no eliminados) con paginación.
     */
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<Usuario> listarUsuarios(org.springframework.data.domain.Pageable pageable) {
        return usuarioRepository.findAllActive(pageable);
    }

    /**
     * Crea un usuario desde el panel de administración con un rol específico.
     */
    @Transactional
    public Usuario crearUsuarioAdmin(CrearUsuarioRequest request, UUID creadorId) {
        if (usuarioRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("El email ya está registrado");
        }

        // Validar RUT único si se proporciona
        if (request.rut() != null && !request.rut().isBlank()) {
            String rutLimpio = request.rut().trim().toUpperCase();
            if (usuarioRepository.findByRut(rutLimpio).isPresent()) {
                throw new IllegalArgumentException("El RUT " + rutLimpio + " ya está registrado");
            }
        }

        if (!request.password().equals(request.confirmarPassword())) {
            throw new IllegalArgumentException("Las contraseñas no coinciden");
        }

        // Validar edad mínima 16 años (Ley 21.719)
        if (request.fechaNacimiento() != null) {
            int edad = Period.between(request.fechaNacimiento(), LocalDate.now()).getYears();
            if (edad < 16) {
                throw new IllegalArgumentException(
                    "Debes tener al menos 16 años para registrarte sin autorización de un representante legal");
            }
        }

        Rol rol = rolRepository.findById(request.rolId())
                .orElseThrow(() -> new IllegalArgumentException("Rol no encontrado"));

        String hashContrasena = passwordEncoder.encode(request.password());
        String rutValue = request.rut() != null ? request.rut().trim().toUpperCase() : null;
        String hashSha256 = HashUtils.sha256(
                (rutValue != null ? rutValue + "|" : "") +
                request.email() + "|" + request.nombreCompleto()
        );

        Usuario usuario = Usuario.builder()
                .rut(rutValue)
                .email(request.email().toLowerCase().trim())
                .nombreCompleto(request.nombreCompleto().trim())
                .hashContrasena(hashContrasena)
                .hashSha256(hashSha256)
                .fechaNacimiento(request.fechaNacimiento())
                .intentosFallidos(0)
                .consentimientoOtorgado(false)
                .roles(Set.of(rol))
                .build();

        usuario = usuarioRepository.save(usuario);

        auditLogger.registrarEvento(
                "USUARIO_CREADO_ADMIN",
                creadorId,
                "CREAR",
                "USUARIO",
                usuario.getId(),
                Map.of("email", usuario.getEmail(), "rol", rol.getNombre())
        );

        return usuario;
    }

    /**
     * Carga masiva de usuarios via CSV.
     * Formato por linea (separador ';'):
     * rut;nombreCompleto;email;password;rol
     */
    @Transactional
    public CargaMasivaUsuariosReport crearUsuariosMasivo(MultipartFile file, UUID creadorId) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("El archivo CSV está vacío");
        }
        List<String> detalleDuplicados = new ArrayList<>();
        List<String> detalleErrores = new ArrayList<>();
        int exitosos = 0;
        int total = 0;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String linea;
            boolean primera = true;
            while ((linea = reader.readLine()) != null) {
                String l = linea.trim();
                if (l.isEmpty()) continue;
                if (primera) {
                    primera = false;
                    if (l.startsWith("\uFEFF")) l = l.substring(1).trim();
                    if (l.toLowerCase().startsWith("rut")) continue;
                }
                total++;
                String[] cols = l.split(";");
                if (cols.length < 5) {
                    detalleErrores.add("Línea " + total + ": formato inválido (se esperan 5 columnas separadas por ';')");
                    continue;
                }
                String rut = cols[0].trim();
                String nombre = cols[1].trim();
                String email = cols[2].trim();
                String password = cols[3].trim();
                String rolNombre = cols[4].trim();

                if (email.isBlank() || nombre.isBlank() || password.isBlank() || rolNombre.isBlank()) {
                    detalleErrores.add("Línea " + total + ": campos vacíos en: " + email);
                    continue;
                }
                if (usuarioRepository.existsByEmail(email.toLowerCase())) {
                    detalleDuplicados.add(email);
                    continue;
                }
                try {
                    String rutLimpio = rut.toUpperCase();
                    if (usuarioRepository.findByRut(rutLimpio).isPresent()) {
                        detalleDuplicados.add(rutLimpio);
                        continue;
                    }
                    Rol rol = rolRepository.findByNombre(rolNombre)
                            .orElseThrow(() -> new IllegalArgumentException("Rol '" + rolNombre + "' no existe"));
                    String hashContrasena = passwordEncoder.encode(password);
                    String hashSha256 = HashUtils.sha256(rutLimpio + "|" + email.toLowerCase() + "|" + nombre);
                    Usuario usuario = Usuario.builder()
                            .rut(rutLimpio)
                            .email(email.toLowerCase().trim())
                            .nombreCompleto(nombre)
                            .hashContrasena(hashContrasena)
                            .hashSha256(hashSha256)
                            .intentosFallidos(0)
                            .consentimientoOtorgado(false)
                            .roles(Set.of(rol))
                            .build();
                    usuario = usuarioRepository.save(usuario);
                    auditLogger.registrarEvento(
                            "USUARIO_CREADO_ADMIN",
                            creadorId,
                            "CREAR",
                            "USUARIO",
                            usuario.getId(),
                            Map.of("email", usuario.getEmail(), "rol", rol.getNombre())
                    );
                    exitosos++;
                } catch (Exception e) {
                    detalleErrores.add("Línea " + total + " (" + email + "): " + e.getMessage());
                }
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Error al leer el archivo CSV: " + e.getMessage());
        }
        return new CargaMasivaUsuariosReport(total, exitosos, detalleDuplicados.size(), detalleErrores.size(), detalleDuplicados, detalleErrores);
    }

    /**
     * Registra un nuevo usuario con rol ESTUDIANTE por defecto.
     */
    public Usuario registrar(RegisterRequest request) {
        if (usuarioRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("El email ya está registrado");
        }

        if (!request.password().equals(request.confirmarPassword())) {
            throw new IllegalArgumentException("Las contraseñas no coinciden");
        }

        if (request.fechaNacimiento() != null) {
            int edad = Period.between(request.fechaNacimiento(), LocalDate.now()).getYears();
            if (edad < 16) {
                throw new IllegalArgumentException(
                    "Debes tener al menos 16 años para registrarte sin autorización de un representante legal");
            }
        }

        Rol rolEstudiante = rolRepository.findByNombre("ESTUDIANTE")
                .orElseThrow(() -> new RuntimeException("Rol ESTUDIANTE no encontrado"));

        String hashContrasena = passwordEncoder.encode(request.password());
        String hashSha256 = HashUtils.sha256(request.email() + "|" + request.nombreCompleto());

        Usuario usuario = Usuario.builder()
                .email(request.email().toLowerCase().trim())
                .nombreCompleto(request.nombreCompleto().trim())
                .hashContrasena(hashContrasena)
                .hashSha256(hashSha256)
                .fechaNacimiento(request.fechaNacimiento())
                .intentosFallidos(0)
                .consentimientoOtorgado(false)
                .roles(Set.of(rolEstudiante))
                .build();

        usuario = usuarioRepository.save(usuario);

        auditLogger.registrarEvento(
                "USUARIO_REGISTRADO",
                usuario.getId(),
                "REGISTRO",
                "USUARIO",
                usuario.getId(),
                Map.of("email", usuario.getEmail(), "rol", "ESTUDIANTE")
        );

        return usuario;
    }

    /**
     * Autentica un usuario por email y contraseña.
     * Verifica bloqueo por intentos fallidos.
     */
    public Optional<Usuario> autenticar(String email, String password) {
        Optional<Usuario> optUsuario = usuarioRepository.findByEmailAndDeletedAtIsNull(email.toLowerCase().trim());

        if (optUsuario.isEmpty()) {
            return Optional.empty();
        }

        Usuario usuario = optUsuario.get();

        // Verificar bloqueo temporal
        if (usuario.getBloqueadoHasta() != null && usuario.getBloqueadoHasta().isAfter(LocalDateTime.now())) {
            auditLogger.registrarEvento(
                    "AUTH_BLOQUEADO",
                    usuario.getId(),
                    "INTENTO_BLOQUEADO",
                    "USUARIO",
                    usuario.getId(),
                    Map.of("email", email, "motivo", "usuario_bloqueado")
            );
            return Optional.empty();
        }

        if (passwordEncoder.matches(password, usuario.getHashContrasena())) {
            // Login exitoso - resetear intentos
            usuario.setIntentosFallidos(0);
            usuario.setUltimoLogin(LocalDateTime.now());
            usuarioRepository.save(usuario);

            auditLogger.registrarEvento(
                    "AUTH_LOGIN_EXITOSO",
                    usuario.getId(),
                    "LOGIN",
                    "USUARIO",
                    usuario.getId(),
                    Map.of("email", email)
            );

            return Optional.of(usuario);
        } else {
            // Login fallido - incrementar contador
            usuario.setIntentosFallidos(usuario.getIntentosFallidos() + 1);

            if (usuario.getIntentosFallidos() >= MAX_INTENTOS_FALLIDOS) {
                usuario.setBloqueadoHasta(LocalDateTime.now().plusMinutes(BLOQUEO_MINUTOS));

                auditLogger.registrarEvento(
                        "AUTH_CUENTA_BLOQUEADA",
                        usuario.getId(),
                        "BLOQUEO_AUTOMATICO",
                        "USUARIO",
                        usuario.getId(),
                        Map.of("email", email, "intentosFallidos", usuario.getIntentosFallidos(), "bloqueadoHasta", usuario.getBloqueadoHasta().toString())
                );
            }

            usuarioRepository.save(usuario);
            return Optional.empty();
        }
    }

    /**
     * Busca usuario activo por email.
     */
    @Transactional(readOnly = true)
    public Optional<Usuario> buscarPorEmail(String email) {
        return usuarioRepository.findByEmailAndDeletedAtIsNull(email.toLowerCase().trim());
    }

    /**
     * Busca usuario activo con consentimiento otorgado.
     */
    @Transactional(readOnly = true)
    public Optional<Usuario> buscarActivoConConsentimiento(String email) {
        return usuarioRepository.findActiveWithConsentimiento(email.toLowerCase().trim());
    }

    /**
     * Busca usuario por ID.
     */
    @Transactional(readOnly = true)
    public Optional<Usuario> buscarPorId(UUID id) {
        return usuarioRepository.findById(id);
    }

    /**
     * Actualiza el consentimiento del usuario.
     */
    public void actualizarConsentimiento(UUID usuarioId, boolean otorgado) {
        usuarioRepository.findById(usuarioId).ifPresent(usuario -> {
            usuario.setConsentimientoOtorgado(otorgado);
            usuarioRepository.save(usuario);
        });
    }

    /**
     * Soft-delete de un usuario.
     */
    public void eliminar(UUID id, UUID eliminadoPor, String motivo) {
        usuarioRepository.findById(id).ifPresent(usuario -> {
            usuario.setDeletedAt(LocalDateTime.now());
            usuario.setDeletedBy(eliminadoPor);
            usuario.setReason(motivo);
            usuario.setEmail(usuario.getEmail() + ".deleted." + System.currentTimeMillis());
            usuarioRepository.save(usuario);

            auditLogger.registrarEvento(
                    "USUARIO_ELIMINADO",
                    eliminadoPor,
                    "SOFT_DELETE",
                    "USUARIO",
                    id,
                    Map.of("motivo", motivo)
            );
        });
    }

    /**
     * Actualiza los datos de un usuario existente.
     * Permite cambiar RUT, nombre, email y rol. Opcionalmente actualiza la contraseña.
     */
    @Transactional
    public Usuario actualizarUsuario(UUID id, EditarUsuarioRequest request, UUID editorId) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        // Validar email único (excluyendo el usuario actual)
        Optional<Usuario> existenteEmail = usuarioRepository.findByEmailAndDeletedAtIsNull(request.email().toLowerCase().trim());
        if (existenteEmail.isPresent() && !existenteEmail.get().getId().equals(id)) {
            throw new IllegalArgumentException("El email " + request.email() + " ya está en uso por otro usuario");
        }

        // Validar RUT único si cambió (excluyendo el usuario actual)
        if (request.rut() != null && !request.rut().isBlank()) {
            String rutLimpio = request.rut().trim().toUpperCase();
            Optional<Usuario> existenteRut = usuarioRepository.findByRut(rutLimpio);
            if (existenteRut.isPresent() && !existenteRut.get().getId().equals(id)) {
                throw new IllegalArgumentException("El RUT " + rutLimpio + " ya está registrado por otro usuario");
            }
            usuario.setRut(rutLimpio);
        } else {
            usuario.setRut(null);
        }

        // Validar contraseña si se proporcionó
        if (request.password() != null && !request.password().isBlank()) {
            if (request.confirmarPassword() == null || !request.password().equals(request.confirmarPassword())) {
                throw new IllegalArgumentException("Las contraseñas no coinciden");
            }
            usuario.setHashContrasena(passwordEncoder.encode(request.password()));
        }

        // Actualizar campos
        usuario.setNombreCompleto(request.nombreCompleto().trim());
        usuario.setEmail(request.email().toLowerCase().trim());

        // Recalcular hash SHA-256
        String hashSha256 = HashUtils.sha256(
                (usuario.getRut() != null ? usuario.getRut() + "|" : "") +
                usuario.getEmail() + "|" + usuario.getNombreCompleto()
        );
        usuario.setHashSha256(hashSha256);

        // Actualizar rol si cambió
        if (request.rolId() != null) {
            Rol nuevoRol = rolRepository.findById(request.rolId())
                    .orElseThrow(() -> new IllegalArgumentException("Rol no encontrado"));
            Set<Rol> rolesActualizados = new HashSet<>();
            rolesActualizados.add(nuevoRol);
            usuario.setRoles(rolesActualizados);
        }

        usuario = usuarioRepository.save(usuario);

        auditLogger.registrarEvento(
                "USUARIO_ACTUALIZADO",
                editorId,
                "ACTUALIZAR",
                "USUARIO",
                id,
                Map.of("email", usuario.getEmail(), "nombre", usuario.getNombreCompleto())
        );

        return usuario;
    }

    /**
     * Soft-delete masivo de usuarios.
     */
    @Transactional
    public void eliminarMasivo(List<UUID> ids, UUID eliminadoPor, String motivo) {
        for (UUID id : ids) {
            eliminar(id, eliminadoPor, motivo);
        }
    }
}
