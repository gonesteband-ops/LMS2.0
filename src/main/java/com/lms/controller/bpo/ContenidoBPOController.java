package com.lms.controller.bpo;

import com.lms.dto.bpo.ContenidoBPOCreateDTO;
import com.lms.mapper.bpo.ContenidoBPOMapper;
import com.lms.service.bpo.ContenidoBPOService;
import com.lms.service.bpo.CuentaBPOService;
import com.lms.util.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Controller
@RequestMapping("/bpo")
public class ContenidoBPOController {

    private final ContenidoBPOService contenidoService;
    private final CuentaBPOService cuentaService;
    private final ContenidoBPOMapper mapper;
    private final SecurityUtils securityUtils;

    public ContenidoBPOController(ContenidoBPOService contenidoService,
                                  CuentaBPOService cuentaService,
                                  ContenidoBPOMapper mapper,
                                  SecurityUtils securityUtils) {
        this.contenidoService = contenidoService;
        this.cuentaService = cuentaService;
        this.mapper = mapper;
        this.securityUtils = securityUtils;
    }

    @GetMapping("/contenidos")
    public String verGestorContenidos(Authentication authentication, Model model) {
        UUID usuarioId = securityUtils.getCurrentUserId(authentication);
        var resumen = contenidoService.obtenerResumen(usuarioId);
        model.addAttribute("resumen", resumen);
        return "bpo/gestor-contenidos";
    }

    @GetMapping("/contenidos/tarjetas")
    public String tarjetasResumen(Authentication authentication, Model model) {
        UUID usuarioId = securityUtils.getCurrentUserId(authentication);
        var resumen = contenidoService.obtenerResumen(usuarioId);
        model.addAttribute("resumen", resumen);
        return "bpo/fragmentos/tarjetas-resumen";
    }

    @GetMapping("/contenidos/lista")
    public String listarContenidos(Authentication authentication,
                                   @RequestParam(required = false) String q,
                                   @RequestParam(required = false) String tipo,
                                   @RequestParam(required = false) String estado,
                                   Model model) {
        UUID usuarioId = securityUtils.getCurrentUserId(authentication);
        var contenidos = contenidoService.buscarConFiltros(usuarioId, q, tipo, estado);
        model.addAttribute("contenidos", contenidos.stream().map(mapper::toResponseDTO).toList());
        model.addAttribute("q", q);
        model.addAttribute("tipo", tipo);
        model.addAttribute("estado", estado);
        return "bpo/fragmentos/lista-contenidos";
    }

    @GetMapping("/contenidos/{id}")
    public String verDetalle(@PathVariable UUID id, Model model) {
        var contenido = contenidoService.buscarPorId(id)
                .orElseThrow(() -> new RuntimeException("Contenido no encontrado"));
        model.addAttribute("contenido", mapper.toResponseDTO(contenido));
        return "bpo/fragmentos/detalle-contenido";
    }

    @GetMapping("/admin/contenidos")
    public String administrarContenidos(Authentication authentication, Model model) {
        UUID usuarioId = securityUtils.getCurrentUserId(authentication);
        var cuentas = cuentaService.obtenerCuentaIdsPorUsuario(usuarioId);
        model.addAttribute("cuentas", cuentas);
        return "redirect:/admin/bpo/gestores";
    }

    @GetMapping("/admin/contenidos/nuevo")
    public String formularioNuevo(Authentication authentication, Model model) {
        model.addAttribute("contenido", new ContenidoBPOCreateDTO());
        model.addAttribute("cuentas", cuentaService.obtenerCuentasActivas());
        return "bpo/admin/formulario-contenido";
    }

    @PostMapping("/admin/contenidos")
    public String crearContenido(Authentication authentication,
                                 @Valid @ModelAttribute ContenidoBPOCreateDTO dto,
                                 @RequestParam UUID cuentaId) {
        UUID usuarioId = securityUtils.getCurrentUserId(authentication);
        contenidoService.crearContenido(dto, usuarioId, cuentaId);
        return "redirect:/bpo/admin/contenidos";
    }

    @PostMapping("/admin/contenidos/{id}/cambiar-estado")
    public String cambiarEstado(Authentication authentication,
                                @PathVariable UUID id,
                                @RequestParam String estado) {
        UUID usuarioId = securityUtils.getCurrentUserId(authentication);
        contenidoService.cambiarEstado(id, estado, usuarioId);
        return "redirect:/bpo/admin/contenidos";
    }
}
