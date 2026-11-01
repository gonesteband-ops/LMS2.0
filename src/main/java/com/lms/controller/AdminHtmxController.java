/*  */package com.lms.controller;

import com.lms.dto.CrearUsuarioRequest;
import com.lms.dto.EditarUsuarioRequest;
import com.lms.model.EventoAuditoria;
import com.lms.model.Rol;
import com.lms.model.Usuario;
import com.lms.repository.EventoAuditoriaRepository;
import com.lms.repository.UsuarioRepository;
import com.lms.service.RolService;
import com.lms.service.UsuarioService;
import com.lms.util.AuditCategoria;
import com.lms.util.SecurityUtils;
import org.springframework.transaction.annotation.Transactional;
import com.lms.repository.InscripcionRepository;
import com.lms.model.Inscripcion;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.BindingResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Controlador MVC que retorna fragmentos HTML para HTMX en las páginas de administración.
 * <p>
 * Alternativa a los endpoints REST puros que devuelven JSON.
 */
@Controller
@RequestMapping("/hx/admin")
@PreAuthorize("hasAnyRole('SUPERADMIN')")
public class AdminHtmxController {

    private static final Logger log = LoggerFactory.getLogger(AdminHtmxController.class);

    private final RolService rolService;
    private final EventoAuditoriaRepository auditoriaRepository;
    private final UsuarioService usuarioService;
    private final UsuarioRepository usuarioRepository;
    private final SecurityUtils securityUtils;
    private final InscripcionRepository inscripcionRepository;

    public AdminHtmxController(RolService rolService,
                                EventoAuditoriaRepository auditoriaRepository,
                                UsuarioService usuarioService,
                                UsuarioRepository usuarioRepository,
                                SecurityUtils securityUtils,
                              InscripcionRepository inscripcionRepository) {
        this.rolService = rolService;
        this.auditoriaRepository = auditoriaRepository;
        this.usuarioService = usuarioService;
        this.usuarioRepository = usuarioRepository;
        this.securityUtils = securityUtils;
        this.inscripcionRepository = inscripcionRepository;
    }

    /**
     * Obtiene un mapa de actorId -> rut para todos los actores únicos en una página de eventos.
     */
    private Map<UUID, String> obtenerRutsActores(Page<EventoAuditoria> eventos) {
        Set<UUID> actorIds = eventos.getContent().stream()
                .map(EventoAuditoria::getActorId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        if (actorIds.isEmpty()) return Map.of();

        List<Usuario> usuarios = usuarioRepository.findAllById(actorIds);
        Map<UUID, String> ruts = new HashMap<>();
        for (Usuario u : usuarios) {
            ruts.put(u.getId(), u.getRut());
        }
        return ruts;
    }

    /**
     * Obtiene un mapa de recursoId -> rut para eventos cuyo recursoTipo es "USUARIO".
     */
    private Map<UUID, String> obtenerRutsRecursos(Page<EventoAuditoria> eventos) {
        Set<UUID> recursoIds = eventos.getContent().stream()
                .filter(e -> "USUARIO".equals(e.getRecursoTipo()))
                .map(EventoAuditoria::getRecursoId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        if (recursoIds.isEmpty()) return Map.of();

        List<Usuario> usuarios = usuarioRepository.findAllById(recursoIds);
        Map<UUID, String> ruts = new HashMap<>();
        for (Usuario u : usuarios) {
            ruts.put(u.getId(), u.getRut());
        }
        return ruts;
    }

    /**
     * Agrega al modelo los mapas de RUTs para actores y recursos de tipo USUARIO.
     */
    private void agregarRutsAlModelo(Model model, Page<EventoAuditoria> eventos) {
        model.addAttribute("eventos", eventos);
        model.addAttribute("rutsActores", obtenerRutsActores(eventos));
        model.addAttribute("rutsRecursos", obtenerRutsRecursos(eventos));
    }

    /**
     * Fragmento HTML con la lista de roles.
     */
    @GetMapping("/roles")
    public String rolesFragment(Model model) {
        java.util.List<Rol> roles = rolService.listarRoles();
        model.addAttribute("roles", roles);
        return "admin/roles-fragment :: roles-list";
    }

    /**
     * Fragmento HTML con la lista de eventos de auditoría.
     */
    @GetMapping("/auditoria")
    public String auditoriaFragment(Model model,
                                     @PageableDefault(size = 50) Pageable pageable) {
        Page<EventoAuditoria> eventos = auditoriaRepository.findAllOrderByCreatedAtDesc(pageable);
        agregarRutsAlModelo(model, eventos);
        return "admin/auditoria-fragment :: audit-list";
    }

    /**
     * Fragmento HTML con eventos de auditoría filtrados por categoría.
     */
    @GetMapping("/auditoria/categoria/{categoriaId}")
    public String auditoriaPorCategoriaFragment(@PathVariable String categoriaId,
                                                 Model model,
                                                 @PageableDefault(size = 50) Pageable pageable) {
        List<String> tipos = AuditCategoria.listarTiposEvento(categoriaId);
        Page<EventoAuditoria> eventos;
        if (tipos == null || tipos.isEmpty()) {
            eventos = auditoriaRepository.findAllOrderByCreatedAtDesc(pageable);
        } else {
            eventos = auditoriaRepository.findByTipoEventoInOrderByCreatedAtDesc(tipos, pageable);
        }
        agregarRutsAlModelo(model, eventos);
        return "admin/auditoria-fragment :: audit-list";
    }

    @GetMapping("/auditoria/buscar")
    public String buscarAuditoria(@RequestParam("q") String query,
                                  Model model,
                                  @PageableDefault(size = 50) Pageable pageable) {
        if (query == null || query.trim().isEmpty()) {
            return auditoriaFragment(model, pageable);
        }
        query = query.trim();
        Page<EventoAuditoria> eventos = auditoriaRepository.buscarEventos(query, pageable);
        agregarRutsAlModelo(model, eventos);
        model.addAttribute("searchQuery", query);
        return "admin/auditoria-fragment :: audit-list";
    }

    /**
     * Fragmento HTML con eventos de auditoría filtrados por tipo.
     */
    @GetMapping("/auditoria/tipo/{tipoEvento}")
    public String auditoriaPorTipoFragment(@PathVariable String tipoEvento,
                                            Model model,
                                            @PageableDefault(size = 50) Pageable pageable) {
        Page<EventoAuditoria> eventos = auditoriaRepository.findByTipoEventoOrderByCreatedAtDesc(tipoEvento, pageable);
        agregarRutsAlModelo(model, eventos);
        return "admin/auditoria-fragment :: audit-list";
    }

    /**
     * Fragmento HTML con opciones del segundo combo (tipos de evento por categoría).
     */
    @GetMapping("/auditoria/categoria/{categoriaId}/opciones-tipos")
    public String opcionesTiposPorCategoria(@PathVariable String categoriaId, Model model) {
        List<String> tipos = AuditCategoria.listarTiposEvento(categoriaId);
        model.addAttribute("tipos", tipos != null ? tipos : List.of());
        model.addAttribute("categoriaId", categoriaId);
        return "admin/auditoria-fragment :: opciones-tipos";
    }

    /**
     * Fragmento HTML con la lista de usuarios activos.
     */
    @GetMapping("/usuarios")
    public String usuariosFragment(Model model,
                                    @PageableDefault(size = 50) Pageable pageable) {
        log.debug("[AdminHtmxController] Solicitando lista de usuarios. Pageable: {}", pageable);
        try {
            Page<Usuario> usuarios = usuarioService.listarUsuarios(pageable);
            log.debug("[AdminHtmxController] Usuarios encontrados: {}. Total pages: {}",
                usuarios.getTotalElements(), usuarios.getTotalPages());
            model.addAttribute("usuarios", usuarios);
        } catch (Exception e) {
            log.error("[AdminHtmxController] Error al listar usuarios: {}", e.getMessage(), e);
            throw e;
        }
        return "admin/usuarios-fragment :: usuarios-list";
    }

    /**
     * Fragmento HTML con el formulario de creación de usuario.
     * Carga los roles disponibles en el selector.
     */
    @GetMapping("/usuarios/crear")
    public String crearUsuarioForm(Model model) {
        java.util.List<Rol> roles = rolService.listarRoles();
        model.addAttribute("roles", roles);
        return "admin/usuarios-crear-form :: crear-usuario-form";
    }

    /**
     * Procesa la creación de un usuario desde el panel de administración.
     * Redirige al listado actualizado.
     */
    @PostMapping("/usuarios")
    public String crearUsuario(@Valid CrearUsuarioRequest request,
                                BindingResult bindingResult,
                                Authentication authentication,
                                Model model,
                                @PageableDefault(size = 50) Pageable pageable) {
        if (bindingResult.hasErrors()) {
            String errorMsg = bindingResult.getAllErrors().stream()
                .map(e -> e.getDefaultMessage())
                .filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.joining(". "));
            java.util.List<Rol> roles = rolService.listarRoles();
            model.addAttribute("roles", roles);
            model.addAttribute("error", errorMsg);
            model.addAttribute("request", request);
            return "admin/usuarios-crear-form :: crear-usuario-form";
        }
        try {
            UUID creadorId = securityUtils.getCurrentUserId(authentication);
            usuarioService.crearUsuarioAdmin(request, creadorId);
            Page<Usuario> usuarios = usuarioService.listarUsuarios(pageable);
            model.addAttribute("usuarios", usuarios);
            model.addAttribute("mensajeExito", "Usuario creado correctamente");
            return "admin/usuarios-fragment :: usuarios-list";
        } catch (IllegalArgumentException e) {
            java.util.List<Rol> roles = rolService.listarRoles();
            model.addAttribute("roles", roles);
            model.addAttribute("error", e.getMessage());
            model.addAttribute("request", request);
            return "admin/usuarios-crear-form :: crear-usuario-form";
        }
    }

    /**
     * Descarga la plantilla CSV de ejemplo para carga masiva de usuarios.
     */
    @GetMapping("/usuarios/plantilla-masiva")
    public ResponseEntity<byte[]> plantillaUsuariosMasiva() {
        String csv = "rut;nombreCompleto;email;password;rol\n" +
                "11.111.111-1;Juan Pérez;juan.perez@example.cl;Password123!;ESTUDIANTE\n" +
                "22.222.222-2;María López;maria.lopez@example.cl;Password123!;ESTUDIANTE\n" +
                "33.333.333-3;Carlos Díaz;carlos.diaz@example.cl;Password123!;INSTRUCTOR\n";
        return ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_TYPE, "text/csv;charset=UTF-8")
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=plantilla-usuarios.csv")
                .body(csv.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Procesa la carga masiva de usuarios via CSV y devuelve el fragmento de reporte.
     */
    @PostMapping("/usuarios/masivo")
    public String cargarUsuariosMasivo(@RequestParam("archivo") MultipartFile archivo,
                                        Authentication authentication,
                                        Model model,
                                        @PageableDefault(size = 50) Pageable pageable) {
        try {
            UUID creadorId = securityUtils.getCurrentUserId(authentication);
            var reporte = usuarioService.crearUsuariosMasivo(archivo, creadorId);
            model.addAttribute("reporteMasivo", reporte);
            Page<Usuario> usuarios = usuarioService.listarUsuarios(pageable);
            model.addAttribute("usuarios", usuarios);
            return "admin/usuarios-fragment :: usuarios-list";
        } catch (IllegalArgumentException e) {
            model.addAttribute("errorMasivo", e.getMessage());
            Page<Usuario> usuarios = usuarioService.listarUsuarios(pageable);
            model.addAttribute("usuarios", usuarios);
            return "admin/usuarios-fragment :: usuarios-list";
        }
    }

    /**
     * Fragmento HTML con el formulario de edición de usuario (pre-cargado).
     */
    @GetMapping("/usuarios/editar/{id}")
    public String editarUsuarioForm(@PathVariable UUID id, Model model) {
        Usuario usuario = usuarioService.buscarPorId(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        java.util.List<Rol> roles = rolService.listarRoles();
        model.addAttribute("usuario", usuario);
        model.addAttribute("roles", roles);
        return "admin/usuarios-editar-form :: editar-usuario-form";
    }

    /**
     * Procesa la edición de un usuario.
     */
    @PostMapping("/usuarios/editar/{id}")
    public String editarUsuario(@PathVariable UUID id,
                                 @Valid EditarUsuarioRequest request,
                                 Authentication authentication,
                                 Model model,
                                 @PageableDefault(size = 50) Pageable pageable) {
        try {
            UUID editorId = securityUtils.getCurrentUserId(authentication);
            usuarioService.actualizarUsuario(id, request, editorId);
            // Éxito: devolver listado actualizado
            Page<Usuario> usuarios = usuarioService.listarUsuarios(pageable);
            model.addAttribute("usuarios", usuarios);
            model.addAttribute("mensajeExito", "Usuario actualizado correctamente");
            return "admin/usuarios-fragment :: usuarios-list";
        } catch (IllegalArgumentException e) {
            // Error: devolver formulario con error
            Usuario usuario = usuarioService.buscarPorId(id).orElse(null);
            java.util.List<Rol> roles = rolService.listarRoles();
            model.addAttribute("usuario", usuario);
            model.addAttribute("roles", roles);
            model.addAttribute("error", e.getMessage());
            model.addAttribute("editRequest", request);
            return "admin/usuarios-editar-form :: editar-usuario-form";
        }
    }

    /**
     * Elimina un usuario individual.
     */
    @PostMapping("/usuarios/eliminar/{id}")
    public String eliminarUsuario(@PathVariable UUID id,
                                   @RequestParam(defaultValue = "Eliminación manual desde administración") String motivo,
                                   Authentication authentication,
                                   Model model,
                                   @PageableDefault(size = 50) Pageable pageable) {
        UUID eliminadoPor = securityUtils.getCurrentUserId(authentication);
        usuarioService.eliminar(id, eliminadoPor, motivo);
        Page<Usuario> usuarios = usuarioService.listarUsuarios(pageable);
        model.addAttribute("usuarios", usuarios);
        model.addAttribute("mensajeExito", "Usuario eliminado correctamente");
        return "admin/usuarios-fragment :: usuarios-list";
    }

    /**
     * Elimina múltiples usuarios seleccionados.
     */
    @PostMapping("/usuarios/eliminar-masivo")
    public String eliminarUsuariosMasivo(@RequestParam List<UUID> ids,
                                          @RequestParam(defaultValue = "Eliminación masiva desde administración") String motivo,
                                          Authentication authentication,
                                          Model model,
                                          @PageableDefault(size = 50) Pageable pageable) {
        UUID eliminadoPor = securityUtils.getCurrentUserId(authentication);
        usuarioService.eliminarMasivo(ids, eliminadoPor, motivo);
        Page<Usuario> usuarios = usuarioService.listarUsuarios(pageable);
        model.addAttribute("usuarios", usuarios);
        model.addAttribute("mensajeExito", ids.size() + " usuario(s) eliminados correctamente");
        return "admin/usuarios-fragment :: usuarios-list";
    }

    /**
     * Crea un nuevo rol desde el panel de administración.
     */
    @PostMapping("/roles")
    public String crearRol(@RequestParam String nombre,
                           @RequestParam(defaultValue = "") String descripcion,
                           @RequestParam(required = false) List<String> permisos,
                           Authentication authentication,
                           Model model) {
        try {
            UUID creadorId = securityUtils.getCurrentUserId(authentication);
            Set<String> permisosSet = permisos != null ? new HashSet<>(permisos) : new HashSet<>();
            rolService.crearRol(nombre, descripcion, permisosSet, creadorId);
            model.addAttribute("mensajeExito", "Rol creado correctamente");
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
        }
        model.addAttribute("roles", rolService.listarRoles());
        model.addAttribute("permisos", rolService.listarPermisos());
        return "admin/roles-fragment :: roles-list";
    }


    @GetMapping("/perfil/cursos")
    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public String perfilCursos(Model model, Authentication authentication) {
        UUID usuarioId = securityUtils.getCurrentUserId(authentication);
        java.util.List<Inscripcion> inscripciones = inscripcionRepository
                .findByUsuarioIdAndDeletedAtIsNull(usuarioId);
        model.addAttribute("inscripciones", inscripciones);
        return "dashboard/cursos-fragment";
    }

}