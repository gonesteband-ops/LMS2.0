package com.lms.controller.bpo;

import com.lms.dto.bpo.ContenidoBPOCreateDTO;
import com.lms.dto.bpo.ContenidoBPOResponseDTO;
import com.lms.mapper.bpo.ContenidoBPOMapper;
import com.lms.model.Usuario;
import com.lms.model.bpo.CuentaBPO;
import com.lms.model.bpo.SubServicioBPO;
import com.lms.model.bpo.UsuarioCuentaBPO;
import com.lms.repository.UsuarioRepository;
import com.lms.repository.bpo.UsuarioCuentaBPORepository;
import com.lms.service.bpo.ContenidoBPOService;
import com.lms.service.bpo.CuentaBPOService;
import com.lms.util.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Controller
@RequestMapping("/admin/bpo")
public class AdminBPOController {

    private final ContenidoBPOService contenidoService;
    private final CuentaBPOService cuentaService;
    private final ContenidoBPOMapper mapper;
    private final SecurityUtils securityUtils;
    private final UsuarioRepository usuarioRepository;
    private final UsuarioCuentaBPORepository usuarioCuentaRepository;

    public AdminBPOController(ContenidoBPOService contenidoService,
                              CuentaBPOService cuentaService,
                              ContenidoBPOMapper mapper,
                              SecurityUtils securityUtils,
                              UsuarioRepository usuarioRepository,
                              UsuarioCuentaBPORepository usuarioCuentaRepository) {
        this.contenidoService = contenidoService;
        this.cuentaService = cuentaService;
        this.mapper = mapper;
        this.securityUtils = securityUtils;
        this.usuarioRepository = usuarioRepository;
        this.usuarioCuentaRepository = usuarioCuentaRepository;
    }

    // Helper para convertir codigo o UUID a UUID
    private UUID resolveCuentaId(String gestorId) {
        try {
            return UUID.fromString(gestorId);
        } catch (IllegalArgumentException e) {
            var cuenta = cuentaService.buscarPorCodigo(gestorId)
                    .orElseThrow(() -> new RuntimeException("Gestor no encontrado: " + gestorId));
            return cuenta.getId();
        }
    }

    // Helper para convertir codigo o UUID a UUID en subcarpetas
    private UUID resolveSubservicioId(UUID cuentaId, String subId) {
        try {
            UUID subIdUuid = UUID.fromString(subId);
            return subIdUuid;
        } catch (IllegalArgumentException e) {
            var sub = cuentaService.obtenerSubserviciosPorCuenta(cuentaId).stream()
                    .filter(s -> subId.equals(s.getCodigo()))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Subcarpeta no encontrada: " + subId));
            return sub.getId();
        }
    }

    @GetMapping("/gestores")
    public String listarGestores() {
        return "redirect:/bpo/gestores";
    }

    @GetMapping("/gestores/nuevo")
    public String formularioNuevoGestor(Model model) {
        model.addAttribute("gestor", new CuentaBPO());
        return "bpo/admin/formulario-gestor";
    }

    @PostMapping("/gestores")
    public String crearGestor(Authentication auth, @Valid @ModelAttribute("gestor") CuentaBPO gestor, Model model) {
        try {
            UUID uid = securityUtils.getCurrentUserId(auth);
            cuentaService.crearCuenta(gestor, uid);
            return "redirect:/admin/bpo/gestores";
        } catch (RuntimeException e) {
            model.addAttribute("gestor", gestor);
            model.addAttribute("error", e.getMessage());
            return "bpo/admin/formulario-gestor";
        }
    }

    @GetMapping("/gestores/{gestorId}")
    public String verGestor(Authentication auth, @PathVariable String gestorId, Model model) {
        UUID uid = securityUtils.getCurrentUserId(auth);
        UUID cuentaUuid = resolveCuentaId(gestorId);
        var cuenta = cuentaService.buscarPorId(cuentaUuid).orElseThrow();
        var subcarpetas = cuentaService.obtenerSubserviciosPorCuenta(cuentaUuid);
        model.addAttribute("gestor", cuenta);
        model.addAttribute("subcarpetas", subcarpetas);
        return "bpo/admin/gestor-detalle";
    }

    @GetMapping("/gestores/{gestorId}/editar")
    public String formularioEditarGestor(Authentication auth, @PathVariable String gestorId, Model model) {
        UUID cuentaUuid = resolveCuentaId(gestorId);
        var gestor = cuentaService.buscarPorId(cuentaUuid).orElseThrow();
        model.addAttribute("gestor", gestor);
        model.addAttribute("gestorId", gestorId);
        return "bpo/admin/formulario-gestor";
    }

    @PostMapping("/gestores/{gestorId}/editar")
    public String actualizarGestor(Authentication auth, @PathVariable String gestorId,
                                    @RequestParam String nombre, @RequestParam String codigo,
                                    @RequestParam(defaultValue = "true") boolean activa, Model model) {
        try {
            UUID cuentaUuid = resolveCuentaId(gestorId);
            cuentaService.actualizarCuenta(cuentaUuid, nombre, codigo, activa);
            return "redirect:/admin/bpo/gestores";
        } catch (RuntimeException e) {
            UUID cuentaUuid = resolveCuentaId(gestorId);
            var gestor = cuentaService.buscarPorId(cuentaUuid).orElseThrow();
            model.addAttribute("gestor", gestor);
            model.addAttribute("gestorId", gestorId);
            model.addAttribute("error", e.getMessage());
            return "bpo/admin/formulario-gestor";
        }
    }

    @PostMapping("/gestores/{gestorId}/toggle-activa")
    public String toggleActiva(Authentication auth, @PathVariable String gestorId) {
        UUID cuentaUuid = resolveCuentaId(gestorId);
        cuentaService.toggleActiva(cuentaUuid);
        return "redirect:/bpo/gestores";
    }

    @PostMapping("/gestores/{gestorId}/eliminar")
    public String eliminarGestor(Authentication auth, @PathVariable String gestorId) {
        UUID uid = securityUtils.getCurrentUserId(auth);
        UUID cuentaUuid = resolveCuentaId(gestorId);
        cuentaService.eliminarCuenta(cuentaUuid, uid, "Eliminado por " + uid);
        return "redirect:/admin/bpo/gestores";
    }

    @GetMapping("/gestores/{gestorId}/subcarpetas/nuevo")
    public String formularioNuevaSubcarpeta(Authentication auth, @PathVariable String gestorId, Model model) {
        UUID cuentaUuid = resolveCuentaId(gestorId);
        var cuenta = cuentaService.buscarPorId(cuentaUuid).orElseThrow();
        model.addAttribute("gestor", cuenta);
        model.addAttribute("subcarpeta", new SubServicioBPO());
        return "bpo/admin/formulario-subcarpeta";
    }

    @PostMapping("/gestores/{gestorId}/subcarpetas")
    public String crearSubcarpeta(Authentication auth, @PathVariable String gestorId,
                                   @ModelAttribute("subcarpeta") SubServicioBPO subcarpeta) {
        UUID cuentaUuid = resolveCuentaId(gestorId);
        cuentaService.crearSubcarpeta(cuentaUuid, subcarpeta.getNombre());
        return "redirect:/admin/bpo/gestores/" + gestorId;
    }

    @GetMapping("/gestores/{gestorId}/subcarpetas/{subId}/editar")
    public String formularioEditarSubcarpeta(Authentication auth, @PathVariable String gestorId,
                                              @PathVariable String subId, Model model) {
        UUID cuentaUuid = resolveCuentaId(gestorId);
        UUID subUuid = resolveSubservicioId(cuentaUuid, subId);
        var cuenta = cuentaService.buscarPorId(cuentaUuid).orElseThrow();
        var sub = cuentaService.obtenerSubserviciosPorCuenta(cuentaUuid).stream()
                .filter(s -> s.getId().equals(subUuid)).findFirst().orElseThrow();
        model.addAttribute("gestor", cuenta);
        model.addAttribute("subcarpeta", sub);
        model.addAttribute("subcarpetaId", subId);
        return "bpo/admin/formulario-subcarpeta";
    }

    @PostMapping("/gestores/{gestorId}/subcarpetas/{subId}/editar")
    public String actualizarSubcarpeta(Authentication auth, @PathVariable String gestorId,
                                        @PathVariable String subId,
                                        @RequestParam String nombre, Model model) {
        UUID uid = securityUtils.getCurrentUserId(auth);
        UUID cuentaUuid = resolveCuentaId(gestorId);
        UUID subUuid = resolveSubservicioId(cuentaUuid, subId);
        cuentaService.actualizarSubcarpeta(subUuid, nombre, uid);
        return "redirect:/admin/bpo/gestores/" + gestorId;
    }

    @PostMapping("/gestores/{gestorId}/subcarpetas/{subId}/eliminar")
    public String eliminarSubcarpeta(Authentication auth, @PathVariable String gestorId,
                                      @PathVariable String subId) {
        UUID uid = securityUtils.getCurrentUserId(auth);
        UUID cuentaUuid = resolveCuentaId(gestorId);
        UUID subUuid = resolveSubservicioId(cuentaUuid, subId);
        cuentaService.eliminarSubcarpeta(subUuid, uid, "Eliminado por " + uid);
        return "redirect:/admin/bpo/gestores/" + gestorId;
    }

    @GetMapping("/gestores/{gestorId}/subcarpetas/{subId}")
    public String verSubcarpeta(Authentication auth, @PathVariable String gestorId,
                                 @PathVariable String subId, Model model) {
        UUID uid = securityUtils.getCurrentUserId(auth);
        UUID cuentaUuid = resolveCuentaId(gestorId);
        UUID subUuid = resolveSubservicioId(cuentaUuid, subId);
        var cuenta = cuentaService.buscarPorId(cuentaUuid).orElseThrow();
        var sub = cuentaService.obtenerSubserviciosPorCuenta(cuentaUuid).stream()
                .filter(s -> s.getId().equals(subUuid)).findFirst().orElseThrow();
        var contenidos = contenidoService.listarPublicacionesPorSubcarpeta(subUuid, 0, 100);
        model.addAttribute("gestor", cuenta);
        model.addAttribute("subcarpeta", sub);
        model.addAttribute("contenidos", contenidos.stream().map(mapper::toResponseDTO).toList());
        return "bpo/admin/subcarpeta-publicaciones";
    }

    @GetMapping("/gestores/{gestorId}/subcarpetas/{subId}/nuevo")
    public String formularioNuevaPublicacion(Authentication auth, @PathVariable String gestorId,
                                              @PathVariable String subId, Model model) {
        UUID cuentaUuid = resolveCuentaId(gestorId);
        UUID subUuid = resolveSubservicioId(cuentaUuid, subId);
        var cuenta = cuentaService.buscarPorId(cuentaUuid).orElseThrow();
        var sub = cuentaService.obtenerSubserviciosPorCuenta(cuentaUuid).stream()
                .filter(s -> s.getId().equals(subUuid)).findFirst().orElseThrow();
        model.addAttribute("gestor", cuenta);
        model.addAttribute("subcarpeta", sub);
        model.addAttribute("publicacion", new ContenidoBPOCreateDTO());
        return "bpo/admin/formulario-publicacion";
    }

    @PostMapping("/gestores/{gestorId}/subcarpetas/{subId}")
    public String crearPublicacion(Authentication auth, @PathVariable String gestorId,
                                      @PathVariable String subId,
                                      @Valid @ModelAttribute("publicacion") ContenidoBPOCreateDTO dto,
                                      BindingResult bindingResult, Model model) {
        UUID cuentaUuid = resolveCuentaId(gestorId);
        UUID subUuid = resolveSubservicioId(cuentaUuid, subId);
        if (bindingResult.hasErrors()) {
            model.addAttribute("gestor", cuentaService.buscarPorId(cuentaUuid).orElseThrow());
            model.addAttribute("subcarpeta", cuentaService.obtenerSubserviciosPorCuenta(cuentaUuid).stream()
                    .filter(s -> s.getId().equals(subUuid)).findFirst().orElseThrow());
            model.addAttribute("publicacion", dto);
            return "bpo/admin/formulario-publicacion";
        }
        UUID uid = securityUtils.getCurrentUserId(auth);
        contenidoService.crearPublicacion(dto, uid, cuentaUuid, subUuid);
        return "redirect:/admin/bpo/gestores/" + gestorId + "/subcarpetas/" + subId;
    }

    @GetMapping("/gestores/{gestorId}/subcarpetas/{subId}/publicaciones/{pubId}/editar")
    public String formularioEditarPublicacion(Authentication auth, @PathVariable String gestorId,
                                               @PathVariable String subId, @PathVariable UUID pubId, Model model) {
        UUID cuentaUuid = resolveCuentaId(gestorId);
        UUID subUuid = resolveSubservicioId(cuentaUuid, subId);
        var cuenta = cuentaService.buscarPorId(cuentaUuid).orElseThrow();
        var sub = cuentaService.obtenerSubserviciosPorCuenta(cuentaUuid).stream()
                .filter(s -> s.getId().equals(subUuid)).findFirst().orElseThrow();
        var contenido = contenidoService.buscarPorId(pubId).orElseThrow();
        
        ContenidoBPOCreateDTO dto = ContenidoBPOCreateDTO.builder()
                .titulo(contenido.getTitulo())
                .descripcionCorta(contenido.getDescripcionCorta())
                .contenidoHtml(contenido.getContenidoHtml())
                .vigenciaDesde(contenido.getVigenciaDesde())
                .vigenciaHasta(contenido.getVigenciaHasta())
                .build();
        
        model.addAttribute("gestor", cuenta);
        model.addAttribute("subcarpeta", sub);
        model.addAttribute("publicacion", dto);
        model.addAttribute("publicacionId", pubId);
        return "bpo/admin/formulario-publicacion";
    }

    @PostMapping("/gestores/{gestorId}/subcarpetas/{subId}/publicaciones/{pubId}")
    public String actualizarPublicacion(Authentication auth, @PathVariable String gestorId,
                                         @PathVariable String subId, @PathVariable UUID pubId,
                                         @Valid @ModelAttribute("publicacion") ContenidoBPOCreateDTO dto,
                                         BindingResult bindingResult, Model model) {
        UUID cuentaUuid = resolveCuentaId(gestorId);
        UUID subUuid = resolveSubservicioId(cuentaUuid, subId);
        if (bindingResult.hasErrors()) {
            model.addAttribute("gestor", cuentaService.buscarPorId(cuentaUuid).orElseThrow());
            model.addAttribute("subcarpeta", cuentaService.obtenerSubserviciosPorCuenta(cuentaUuid).stream()
                    .filter(s -> s.getId().equals(subUuid)).findFirst().orElseThrow());
            model.addAttribute("publicacion", dto);
            model.addAttribute("publicacionId", pubId);
            return "bpo/admin/formulario-publicacion";
        }
        UUID uid = securityUtils.getCurrentUserId(auth);
        contenidoService.actualizarPublicacion(pubId, dto, uid);
        return "redirect:/admin/bpo/gestores/" + gestorId + "/subcarpetas/" + subId;
    }

    @PostMapping("/publicaciones/{id}/eliminar")
    public String eliminarPublicacion(Authentication auth, @PathVariable UUID id,
                                       @RequestParam(defaultValue = "") String motivo,
                                       @RequestParam("gestorId") String gestorId,
                                       @RequestParam("subId") String subId) {
        UUID uid = securityUtils.getCurrentUserId(auth);
        contenidoService.eliminarPublicacion(id, uid, motivo);
        return "redirect:/admin/bpo/gestores/" + gestorId + "/subcarpetas/" + subId;
    }

    @GetMapping("/gestores/{gestorId}/compartir")
    public String formularioCompartir(Authentication auth, @PathVariable String gestorId, Model model) {
        UUID uid = securityUtils.getCurrentUserId(auth);
        UUID cuentaUuid = resolveCuentaId(gestorId);
        var gestor = cuentaService.buscarPorId(cuentaUuid).orElseThrow();
        var asignados = usuarioCuentaRepository.findByCuentaId(cuentaUuid);
        model.addAttribute("gestor", gestor);
        model.addAttribute("asignados", asignados);
        return "bpo/admin/compartir-gestor";
    }

    @PostMapping("/gestores/{gestorId}/compartir")
    public String compartirGestor(Authentication auth, @PathVariable String gestorId,
                                   @RequestParam String usuarioId, Model model) {
        UUID uid = securityUtils.getCurrentUserId(auth);
        UUID cuentaUuid = resolveCuentaId(gestorId);
        var gestor = cuentaService.buscarPorId(cuentaUuid).orElseThrow();
        try {
            // Resolver identificador: UUID -> RUT -> email
            UUID usuarioUuid = null;
            try {
                usuarioUuid = UUID.fromString(usuarioId);
            } catch (IllegalArgumentException e) {
                // Intentar como RUT
                var porRut = usuarioRepository.findByRutAndDeletedAtIsNull(usuarioId);
                if (porRut.isPresent()) {
                    usuarioUuid = porRut.get().getId();
                } else {
                    // Intentar como email
                    var porEmail = usuarioRepository.findByEmailAndDeletedAtIsNull(usuarioId);
                    if (porEmail.isPresent()) {
                        usuarioUuid = porEmail.get().getId();
                    } else {
                        throw new RuntimeException("Usuario no encontrado con ese RUT o email");
                    }
                }
            }
            cuentaService.asignarUsuarioACuenta(usuarioUuid, cuentaUuid, uid);
            model.addAttribute("mensajeExito", "Gestor compartido correctamente");
        } catch (RuntimeException e) {
            model.addAttribute("error", e.getMessage());
        }
        var asignados = usuarioCuentaRepository.findByCuentaId(cuentaUuid);
        model.addAttribute("gestor", gestor);
        model.addAttribute("asignados", asignados);
        return "bpo/admin/compartir-gestor";
    }

    @PostMapping("/gestores/{gestorId}/compartir/{usuarioId}/remover")
    public String removerCompartir(Authentication auth, @PathVariable String gestorId,
                                    @PathVariable UUID usuarioId) {
        UUID cuentaUuid = resolveCuentaId(gestorId);
        usuarioCuentaRepository.deleteById(new com.lms.model.bpo.UsuarioCuentaBPOId(usuarioId, cuentaUuid));
        return "redirect:/admin/bpo/gestores/" + gestorId + "/compartir";
    }
}
