package com.lms.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.lms.service.CertificacionService;
import com.lms.service.InscripcionService;
import com.lms.util.SecurityUtils;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Comparator;
import java.util.UUID;

@Controller
@RequestMapping("/admin/certificaciones")
public class AdminCertificacionController {

    private final CertificacionService certificacionService;
    private final InscripcionService inscripcionService;
    private final SecurityUtils securityUtils;

    public AdminCertificacionController(CertificacionService certificacionService,
                                         InscripcionService inscripcionService,
                                         SecurityUtils securityUtils) {
        this.certificacionService = certificacionService;
        this.inscripcionService = inscripcionService;
        this.securityUtils = securityUtils;
    }

    @GetMapping
    public String listar(Model model, @RequestParam(defaultValue = "titulo") String sort) {
        var lista = new java.util.ArrayList<>(certificacionService.listarTodas());
        switch (sort) {
            case "titulo_desc":
                lista.sort((a, b) -> b.getTitulo().compareToIgnoreCase(a.getTitulo()));
                break;
            case "codigo":
                lista.sort((a, b) -> {
                    if (a.getCodigo() == null) return 1;
                    if (b.getCodigo() == null) return -1;
                    return a.getCodigo().compareToIgnoreCase(b.getCodigo());
                });
                break;
            case "fecha":
                lista.sort(Comparator.comparing(a -> a.getCreatedAt() != null ? a.getCreatedAt() : java.time.LocalDateTime.MIN));
                break;
            case "fecha_desc":
                lista.sort((a, b) -> {
                    if (a.getCreatedAt() == null) return 1;
                    if (b.getCreatedAt() == null) return -1;
                    return b.getCreatedAt().compareTo(a.getCreatedAt());
                });
                break;
            default:
                lista.sort((a, b) -> a.getTitulo().compareToIgnoreCase(b.getTitulo()));
                break;
        }
        model.addAttribute("certificaciones", lista);
        model.addAttribute("sort", sort);
        return "certificaciones/listado";
    }

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    @GetMapping("/crear")
    public String formularioCrear(Model model) {
        model.addAttribute("certificacion", null);
        model.addAttribute("preguntasJson", "[]");
        return "certificaciones/editor";
    }

    @GetMapping("/{id}/editar")
    public String formularioEditar(@PathVariable UUID id, Model model) {
        var cert = certificacionService.buscarPorId(id);
        model.addAttribute("certificacion", cert);
        try {
            model.addAttribute("preguntasJson", cert.getPreguntas() != null
                    ? objectMapper.writeValueAsString(cert.getPreguntas()) : "[]");
        } catch (Exception e) {
            model.addAttribute("preguntasJson", "[]");
        }
        return "certificaciones/editor";
    }

    @GetMapping("/{id}/participantes")
    public String participantes(@PathVariable UUID id, Model model) {
        model.addAttribute("certificacion", certificacionService.buscarPorId(id));
        model.addAttribute("inscritos", certificacionService.listarInscritos(id));
        return "certificaciones/participantes";
    }

    @PostMapping("/{id}/inscribir")
    public String inscribir(@PathVariable UUID id, @RequestParam String usuarioId,
                             Authentication auth, RedirectAttributes ra) {
        UUID uid = securityUtils.getCurrentUserId(auth);
        try {
            UUID usuarioUuid = inscripcionService.resolverIdentificador(usuarioId);
            certificacionService.inscribirUsuario(id, usuarioUuid, uid);
            ra.addFlashAttribute("mensajeExito", "Usuario inscrito correctamente");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/certificaciones/" + id + "/participantes";
    }

    @PostMapping("/{id}/inscribir-masivo")
    public String inscribirMasivo(@PathVariable UUID id, @RequestParam MultipartFile archivo,
                                   RedirectAttributes ra) {
        try {
            var reporte = certificacionService.inscribirMasivo(id, archivo);
            ra.addFlashAttribute("reporte", reporte);
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/certificaciones/" + id + "/participantes";
    }

    @PostMapping("/{id}/inscribir/{usuarioId}/remover")
    public String remover(@PathVariable UUID id, @PathVariable UUID usuarioId) {
        certificacionService.removerInscripcion(id, usuarioId);
        return "redirect:/admin/certificaciones/" + id + "/participantes";
    }
}
