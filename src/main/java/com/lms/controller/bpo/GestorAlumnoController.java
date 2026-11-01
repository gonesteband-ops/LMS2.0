package com.lms.controller.bpo;

import com.lms.mapper.bpo.ContenidoBPOMapper;
import com.lms.model.bpo.CuentaBPO;
import com.lms.service.bpo.ContenidoBPOService;
import com.lms.service.bpo.CuentaBPOService;
import com.lms.util.SecurityUtils;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

@Controller
@RequestMapping("/bpo")
public class GestorAlumnoController {

    private final ContenidoBPOService contenidoService;
    private final CuentaBPOService cuentaService;
    private final ContenidoBPOMapper mapper;
    private final SecurityUtils securityUtils;

    public GestorAlumnoController(ContenidoBPOService contenidoService,
                                   CuentaBPOService cuentaService,
                                   ContenidoBPOMapper mapper,
                                   SecurityUtils securityUtils) {
        this.contenidoService = contenidoService;
        this.cuentaService = cuentaService;
        this.mapper = mapper;
        this.securityUtils = securityUtils;
    }

    @GetMapping("/gestores")
    public String listarGestores(Authentication auth, Model model,
                                  @RequestParam(defaultValue = "") String q,
                                  @RequestParam(defaultValue = "nombre") String sort) {
        UUID uid = securityUtils.getCurrentUserId(auth);
        boolean esAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_SUPERADMIN") || a.getAuthority().equals("ROLE_ADMIN"));
        List<CuentaBPO> gestores;
        Stream<CuentaBPO> stream;
        if (esAdmin) {
            gestores = cuentaService.obtenerTodasLasCuentas();
            stream = gestores.stream();
        } else {
            gestores = cuentaService.obtenerCuentasActivas();
            var asignados = cuentaService.obtenerCuentaIdsPorUsuario(uid);
            stream = gestores.stream().filter(g -> asignados.contains(g.getId()));
        }
        if (!q.isBlank()) {
            String qLower = q.toLowerCase().trim();
            stream = stream.filter(g -> g.getNombre().toLowerCase().contains(qLower)
                    || (g.getCodigo() != null && g.getCodigo().toLowerCase().contains(qLower)));
        }
        var lista = stream.toList();
        switch (sort) {
            case "nombre_desc":
                lista = lista.stream().sorted((a, b) -> b.getNombre().compareToIgnoreCase(a.getNombre())).toList();
                break;
            case "fecha":
                lista = lista.stream().sorted((a, b) -> {
                    if (a.getCreatedAt() == null) return 1;
                    if (b.getCreatedAt() == null) return -1;
                    return a.getCreatedAt().compareTo(b.getCreatedAt());
                }).toList();
                break;
            case "fecha_desc":
                lista = lista.stream().sorted((a, b) -> {
                    if (a.getCreatedAt() == null) return 1;
                    if (b.getCreatedAt() == null) return -1;
                    return b.getCreatedAt().compareTo(a.getCreatedAt());
                }).toList();
                break;
            default:
                lista = lista.stream().sorted((a, b) -> a.getNombre().compareToIgnoreCase(b.getNombre())).toList();
                break;
        }
        model.addAttribute("gestores", lista);
        model.addAttribute("q", q);
        model.addAttribute("sort", sort);
        return "bpo/gestores";
    }

    @GetMapping("/gestores/{gestorId}")
    public String verGestor(Authentication auth, @PathVariable UUID gestorId, Model model) {
        UUID uid = securityUtils.getCurrentUserId(auth);
        var cuenta = cuentaService.buscarPorId(gestorId).orElseThrow();
        var agrupado = contenidoService.listarAgrupado(uid, gestorId, null);
        var subcarpetas = cuentaService.obtenerSubserviciosPorCuenta(gestorId);
        model.addAttribute("gestor", cuenta);
        model.addAttribute("agrupado", agrupado);
        model.addAttribute("subcarpetas", subcarpetas);
        return "bpo/gestor-detalle";
    }

    @GetMapping("/gestores/contar")
    @ResponseBody
    public long contarGestores(Authentication auth) {
        UUID uid = securityUtils.getCurrentUserId(auth);
        var gestores = cuentaService.obtenerCuentasActivas();
        var asignados = cuentaService.obtenerCuentaIdsPorUsuario(uid);
        return gestores.stream().filter(g -> asignados.contains(g.getId())).count();
    }

    @GetMapping("/publicaciones/{pubId}")
    public String verPublicacion(Authentication auth, @PathVariable UUID pubId, Model model) {
        var pub = contenidoService.buscarPorId(pubId)
                .orElseThrow(() -> new RuntimeException("Publicación no encontrada"));
        model.addAttribute("pub", mapper.toResponseDTO(pub));
        return "bpo/fragmentos/detalle-publicacion :: detalle";
    }

    @GetMapping("/gestores/{gestorId}/buscar")
    public String buscar(Authentication auth, @PathVariable UUID gestorId,
                          @RequestParam(required = false) String q, Model model) {
        UUID uid = securityUtils.getCurrentUserId(auth);
        var cuenta = cuentaService.buscarPorId(gestorId).orElseThrow();
        var agrupado = contenidoService.listarAgrupado(uid, gestorId, q);
        model.addAttribute("gestor", cuenta);
        model.addAttribute("agrupado", agrupado);
        model.addAttribute("q", q);
        return "bpo/fragmentos/columnas-publicaciones";
    }
}
