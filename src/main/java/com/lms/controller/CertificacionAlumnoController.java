package com.lms.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lms.dto.IntentoCertificacionResponse;
import com.lms.repository.CertificacionUsuarioRepository;
import com.lms.service.CertificacionService;
import com.lms.util.SecurityUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@Controller
@RequestMapping("/certificaciones")
@PreAuthorize("isAuthenticated()")
public class CertificacionAlumnoController {

    private final CertificacionService certificacionService;
    private final CertificacionUsuarioRepository certUsuarioRepo;
    private final SecurityUtils securityUtils;

    public CertificacionAlumnoController(CertificacionService certificacionService,
                                          CertificacionUsuarioRepository certUsuarioRepo,
                                          SecurityUtils securityUtils) {
        this.certificacionService = certificacionService;
        this.certUsuarioRepo = certUsuarioRepo;
        this.securityUtils = securityUtils;
    }

    @GetMapping
    public String listarMisCertificaciones(Authentication auth, Model model) {
        boolean esEstudiante = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ESTUDIANTE"));
        if (!esEstudiante) {
            return "redirect:/admin/certificaciones";
        }
        UUID uid = securityUtils.getCurrentUserId(auth);
        var certs = certificacionService.listarMisCertificaciones(uid);
        Map<UUID, com.lms.dto.IntentoCertificacionResponse> ultimosIntentos = new java.util.HashMap<>();
        for (var c : certs) {
            var resultados = certificacionService.resultados(c.getId(), uid);
            if (!resultados.isEmpty()) {
                ultimosIntentos.put(c.getId(), resultados.get(resultados.size() - 1));
            }
        }
        model.addAttribute("certificaciones", certs);
        model.addAttribute("ultimosIntentos", ultimosIntentos);
        return "certificaciones/mis-certificaciones";
    }

    private final ObjectMapper objectMapper = new ObjectMapper();

    @GetMapping("/{id}/rendir")
    @PreAuthorize("hasRole('ESTUDIANTE')")
    public String rendir(@PathVariable UUID id, Authentication auth, Model model) {
        UUID uid = securityUtils.getCurrentUserId(auth);
        var inscripcion = certUsuarioRepo.findByCertificacionIdAndUsuarioId(id, uid);
        if (inscripcion.isEmpty()) {
            return "redirect:/certificaciones?error=no-inscrito";
        }
        var cert = certificacionService.buscarPorId(id);
        var resultados = certificacionService.resultados(id, uid);
        var completado = resultados.stream().filter(r -> "COMPLETADO".equals(r.getEstado())).findFirst();
        if (completado.isPresent()) {
            model.addAttribute("certificacion", cert);
            model.addAttribute("intento", completado.get());
            return "certificaciones/rendir";
        }
        var preguntas = certificacionService.obtenerPreguntasSanitizadas(id);
        var intento = certificacionService.buscarIntentoEnCurso(id, uid);
        try {
            model.addAttribute("preguntasJson", objectMapper.writeValueAsString(preguntas));
        } catch (Exception e) {
            model.addAttribute("preguntasJson", "[]");
        }
        model.addAttribute("certificacion", cert);
        model.addAttribute("preguntas", preguntas);
        model.addAttribute("intento", intento);
        return "certificaciones/rendir";
    }

    @GetMapping("/contar")
    @ResponseBody
    public long contar(Authentication auth) {
        UUID uid = securityUtils.getCurrentUserId(auth);
        return certificacionService.contarMisCertificaciones(uid);
    }
}
