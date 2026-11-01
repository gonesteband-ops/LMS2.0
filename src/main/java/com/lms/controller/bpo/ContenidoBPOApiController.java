package com.lms.controller.bpo;

import com.lms.dto.bpo.ContenidoBPOCreateDTO;
import com.lms.dto.bpo.ContenidoBPOResponseDTO;
import com.lms.mapper.bpo.ContenidoBPOMapper;
import com.lms.model.bpo.ContenidoBPO;
import com.lms.model.bpo.CuentaBPO;
import com.lms.service.bpo.ContenidoBPOService;
import com.lms.service.bpo.CuentaBPOService;
import com.lms.util.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/bpo/contenidos")
public class ContenidoBPOApiController {

    private final ContenidoBPOService contenidoService;
    private final CuentaBPOService cuentaService;
    private final ContenidoBPOMapper mapper;
    private final SecurityUtils securityUtils;

    public ContenidoBPOApiController(ContenidoBPOService contenidoService,
                                     CuentaBPOService cuentaService,
                                     ContenidoBPOMapper mapper,
                                     SecurityUtils securityUtils) {
        this.contenidoService = contenidoService;
        this.cuentaService = cuentaService;
        this.mapper = mapper;
        this.securityUtils = securityUtils;
    }

    @PostMapping("/importar")
    @PreAuthorize("hasAuthority('BPO_CONTENIDO_IMPORTAR')")
    public ResponseEntity<ContenidoBPOResponseDTO> importar(
            Authentication authentication,
            @Valid @RequestBody ContenidoBPOCreateDTO dto) {

        UUID usuarioId = securityUtils.getCurrentUserId(authentication);

        CuentaBPO cuenta = cuentaService.buscarPorCodigo(dto.getTitulo())
                .orElseThrow(() -> new IllegalArgumentException("Cuenta no encontrada: " + dto.getTitulo()));

        ContenidoBPO contenido = contenidoService.crearContenido(dto, usuarioId, cuenta.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toResponseDTO(contenido));
    }
}
