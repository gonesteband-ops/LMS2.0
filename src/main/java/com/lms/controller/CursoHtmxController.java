package com.lms.controller;

import com.lms.service.CursoService;
import com.lms.util.SecurityUtils;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Collection;
import java.util.UUID;

/**
 * Controlador MVC que retorna fragmentos HTML para HTMX.
 * Alternativa a los endpoints REST puros que devuelven JSON.
 */
@Controller
@RequestMapping("/hx/cursos")
public class CursoHtmxController {

    private final CursoService cursoService;
    private final SecurityUtils securityUtils;

    public CursoHtmxController(CursoService cursoService, SecurityUtils securityUtils) {
        this.cursoService = cursoService;
        this.securityUtils = securityUtils;
    }

    /**
     * Retorna el fragmento HTML con la lista de cursos paginada.
     * Usado por HTMX en listado.html para reemplazar el div#cursos-list.
     */
    @GetMapping
    public String listadoFragment(Model model,
                                  @PageableDefault(size = 20) Pageable pageable,
                                  Authentication authentication) {
        UUID usuarioId = securityUtils.getCurrentUserId(authentication);
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        model.addAttribute("cursos", cursoService.listarCursosSegunRol(usuarioId, authorities, pageable));
        return "cursos/fragmentos :: listado";
    }
}
