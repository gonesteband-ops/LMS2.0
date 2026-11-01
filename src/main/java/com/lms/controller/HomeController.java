package com.lms.controller;

import com.lms.dto.ConsentimientoRequest;
import com.lms.model.PaqueteScorm;
import com.lms.model.Usuario;
import com.lms.service.ConsentimientoService;
import com.lms.service.RolService;
import com.lms.service.InscripcionService;
import com.lms.service.ScormService;
import com.lms.service.UsuarioService;
import com.lms.util.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Controlador de la página principal y navegación pública.
 * <p>
 * Sirve templates Thymeleaf con HTMX para interacción dinámica.
 */
@Controller
public class HomeController {

    private final ScormService scormService;
    private final UsuarioService usuarioService;
    private final SecurityUtils securityUtils;
    private final ConsentimientoService consentimientoService;
    private final RolService rolService;
    private final InscripcionService inscripcionService;

    public HomeController(ScormService scormService,
                          UsuarioService usuarioService,
                          SecurityUtils securityUtils,
                          ConsentimientoService consentimientoService,
                          RolService rolService,
                          InscripcionService inscripcionService) {
        this.scormService = scormService;
        this.usuarioService = usuarioService;
        this.securityUtils = securityUtils;
        this.consentimientoService = consentimientoService;
        this.rolService = rolService;
        this.inscripcionService = inscripcionService;
    }

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("titulo", "LMS LegalTech - Sistema de Gestión de Aprendizaje");
        model.addAttribute("descripcion", "Plataforma de aprendizaje con cumplimiento legal (Leyes 19.799 y 21.719)");
        return "index";
    }

    @GetMapping("/login")
    public String login(Model model) {
        model.addAttribute("titulo", "Iniciar Sesión - LMS LegalTech");
        return "auth/login";
    }

    @GetMapping("/registro")
    public String registro(Model model) {
        model.addAttribute("titulo", "Registro - LMS LegalTech");
        return "auth/registro";
    }

    @GetMapping("/dashboard")
    public String dashboard(Authentication authentication, Model model) {
        if (authentication != null && authentication.isAuthenticated()) {
            UUID userId = securityUtils.getCurrentUserId(authentication);
            if (!consentimientoService.haOtorgadoConsentimiento(userId, "CUENTA")) {
                return "redirect:/consentimiento";
            }
            boolean esEstudiante = authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ESTUDIANTE"));
            boolean esStaff = authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_SUPERADMIN")
                            || a.getAuthority().equals("ROLE_ADMIN")
                            || a.getAuthority().equals("ROLE_INSTRUCTOR"));
            String certificacionesUrl = esEstudiante && !esStaff
                    ? "/certificaciones"
                    : "/admin/certificaciones";
            model.addAttribute("certificacionesUrl", certificacionesUrl);
        }
        model.addAttribute("titulo", "Dashboard - LMS LegalTech");
        return "dashboard/index";
    }

    @GetMapping("/dashboard/notificaciones")
    public String dashboardNotificaciones(Model model) {
        model.addAttribute("titulo", "Notificaciones - LMS LegalTech");
        return "dashboard/notificaciones";
    }

    @GetMapping("/consentimiento")
    public String consentimiento(Authentication authentication, Model model,
                                  @RequestParam(name = "editar", defaultValue = "false") boolean editar) {
        model.addAttribute("titulo", "Consentimiento - LMS LegalTech");
        if (authentication != null && authentication.isAuthenticated()) {
            UUID userId = securityUtils.getCurrentUserId(authentication);
            boolean otorgado = editar ? false : consentimientoService.haOtorgadoConsentimiento(userId, "CUENTA");
            model.addAttribute("consentimientoOtorgado", otorgado);
            model.addAttribute("consentimientoCUENTA", consentimientoService.haOtorgadoConsentimiento(userId, "CUENTA"));
            model.addAttribute("consentimientoMARKETING", consentimientoService.haOtorgadoConsentimiento(userId, "MARKETING"));
            model.addAttribute("consentimientoANALISIS", consentimientoService.haOtorgadoConsentimiento(userId, "ANALISIS"));
            model.addAttribute("consentimientoCOMPARTIR_TERCEROS", consentimientoService.haOtorgadoConsentimiento(userId, "COMPARTIR_TERCEROS"));
        }
        return "auth/consentimiento";
    }

    @PostMapping("/consentimiento")
    public String procesarConsentimiento(
            @RequestParam(value = "finalidad_CUENTA", defaultValue = "false") boolean cuenta,
            @RequestParam(value = "finalidad_MARKETING", defaultValue = "false") boolean marketing,
            @RequestParam(value = "finalidad_ANALISIS", defaultValue = "false") boolean analisis,
            @RequestParam(value = "finalidad_COMPARTIR_TERCEROS", defaultValue = "false") boolean compartir,
            @RequestParam String versionPolitica,
            @RequestParam String idioma,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        if (!cuenta) {
            return "redirect:/consentimiento?error=debe-aceptar-cuenta";
        }
        UUID usuarioId = securityUtils.getCurrentUserId(authentication);
        Map<String, Boolean> finalidades = new HashMap<>();
        finalidades.put("CUENTA", true);
        finalidades.put("MARKETING", marketing);
        finalidades.put("ANALISIS", analisis);
        finalidades.put("COMPARTIR_TERCEROS", compartir);
        ConsentimientoRequest request = new ConsentimientoRequest(versionPolitica, idioma, finalidades);
        consentimientoService.registrarConsentimiento(usuarioId, request, httpRequest);
        return "redirect:/dashboard";
    }

    // ========================================
    // SECCIONES DEL DASHBOARD
    // ========================================

    @GetMapping("/dashboard/documentos")
    public String documentos(Model model) {
        model.addAttribute("titulo", "Documentos - LMS LegalTech");
        return "dashboard/documentos";
    }

    @GetMapping("/dashboard/firmas")
    public String firmas(Model model) {
        model.addAttribute("titulo", "Mis Firmas Electrónicas - LMS LegalTech");
        return "dashboard/firmas";
    }

    @GetMapping("/dashboard/perfil")
    public String perfil(Model model) {
        model.addAttribute("titulo", "Mi Perfil - LMS LegalTech");
        return "dashboard/perfil";
    }

    @GetMapping("/dashboard/arcop")
    public String arcop(Model model) {
        model.addAttribute("titulo", "Mis Datos ARCOP - LMS LegalTech");
        return "dashboard/arcop";
    }

    @GetMapping("/dashboard/privacidad")
    public String privacidad(Authentication authentication, Model model) {
        model.addAttribute("titulo", "Privacidad - LMS LegalTech");
        if (authentication != null) {
            UUID usuarioId = securityUtils.getCurrentUserId(authentication);
            usuarioService.buscarPorId(usuarioId).ifPresent(u ->
                model.addAttribute("usuario", u));
        }
        return "dashboard/privacidad";
    }

    @GetMapping("/dashboard/exportaciones")
    public String exportaciones(Model model) {
        model.addAttribute("titulo", "Exportaciones - LMS LegalTech");
        return "dashboard/exportaciones";
    }

    @GetMapping("/dashboard/evaluaciones-pendientes")
    public String evaluacionesPendientes(Model model) {
        model.addAttribute("titulo", "Evaluaciones Pendientes - LMS LegalTech");
        return "dashboard/evaluaciones-pendientes";
    }

    // ========================================
    // SECCIONES DE CURSOS
    // ========================================

    @GetMapping("/cursos")
    public String cursos(Model model) {
        model.addAttribute("titulo", "Cursos - LMS LegalTech");
        return "cursos/listado";
    }

    @GetMapping("/cursos/crear")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'INSTRUCTOR')")
    public String crearCurso(Model model) {
        model.addAttribute("titulo", "Crear Curso - LMS LegalTech");
        return "cursos/editor";
    }

    @GetMapping("/cursos/editar")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'INSTRUCTOR')")
    public String editarCurso(Model model) {
        model.addAttribute("titulo", "Editar Curso - LMS LegalTech");
        return "cursos/editor";
    }

    @GetMapping("/cursos/documentos")
    public String documentosCurso(Model model) {
        model.addAttribute("titulo", "Documentos del Curso - LMS LegalTech");
        return "cursos/documentos";
    }

    @GetMapping("/cursos/participantes")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'INSTRUCTOR')")
    public String participantesCurso(Model model) {
        model.addAttribute("titulo", "Inscribir Participantes - LMS LegalTech");
        return "cursos/participantes";
    }

    @GetMapping("/cursos/evaluaciones")
    public String listarEvaluaciones(Model model, Authentication authentication) {
        model.addAttribute("titulo", "Evaluaciones - LMS LegalTech");
        // Pasar rol al template para que JS detecte correctamente si es estudiante
        boolean esEstudiante = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ESTUDIANTE"));
        model.addAttribute("esEstudiante", esEstudiante);
        return "evaluaciones/listado";
    }

    @GetMapping("/cursos/evaluaciones/crear")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'INSTRUCTOR')")
    public String crearEvaluacion(Model model) {
        model.addAttribute("titulo", "Crear Evaluación - LMS LegalTech");
        return "evaluaciones/editor";
    }

    @GetMapping("/cursos/scorm")
    public String bibliotecaScorm(Model model) {
        model.addAttribute("titulo", "Biblioteca SCORM - LMS LegalTech");
        return "cursos/scorm-upload";
    }

    @GetMapping("/cursos/scorm/{paqueteId}/play")
    public String reproducirScorm(@PathVariable UUID paqueteId, Model model) {
        PaqueteScorm paquete = scormService.obtenerManifiesto(paqueteId);
        model.addAttribute("paqueteId", paquete.getId().toString());
        model.addAttribute("titulo", paquete.getTitulo());
        model.addAttribute("versionScorm", paquete.getVersionScorm());
        model.addAttribute("urlIndex", paquete.getUrlIndex());
        return "cursos/scorm-player";
    }

    // ========================================
    // SECCIONES DE ADMINISTRACIÓN
    // ========================================

    @GetMapping("/admin/usuarios")
    @PreAuthorize("hasAnyRole('SUPERADMIN')")
    public String adminUsuarios(Model model) {
        model.addAttribute("titulo", "Administración de Usuarios - LMS LegalTech");
        return "admin/usuarios";
    }

    @GetMapping("/admin/auditoria")
    @PreAuthorize("hasAnyRole('SUPERADMIN')")
    public String adminAuditoria(Model model) {
        model.addAttribute("titulo", "Auditoría del Sistema - LMS LegalTech");
        return "admin/auditoria";
    }

    @GetMapping("/admin/incidentes")
    public String adminIncidentes(Model model) {
        model.addAttribute("titulo", "Incidentes de Seguridad - LMS LegalTech");
        return "admin/incidentes";
    }

    @GetMapping("/admin/roles")
    public String adminRoles(Model model) {
        model.addAttribute("titulo", "Roles y Permisos - LMS LegalTech");
        model.addAttribute("permisos", rolService.listarPermisos());
        return "admin/roles";
    }

    // ========================================
    // RENDIR EVALUACIÓN (vista para estudiantes)
    // ========================================

    @GetMapping("/cursos/evaluaciones/{evaluacionId}/rendir")
    public String rendirEvaluacion(@PathVariable UUID evaluacionId, Model model) {
        model.addAttribute("titulo", "Rendir Evaluación - LMS LegalTech");
        return "evaluaciones/rendir";
    }

    // ========================================
    // REPRODUCTOR MULTIMEDIA
    // ========================================

    @GetMapping("/cursos/reproductor")
    public String reproductor(Model model) {
        model.addAttribute("titulo", "Reproductor Multimedia - LMS LegalTech");
        return "dashboard/reproductor";
    }
}
