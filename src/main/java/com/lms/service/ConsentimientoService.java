package com.lms.service;

import com.lms.dto.ConsentimientoRequest;
import com.lms.model.Consentimiento;
import com.lms.model.Consentimiento.Finalidad;
import com.lms.model.Usuario;
import com.lms.repository.ConsentimientoRepository;
import com.lms.util.AuditLogger;
import com.lms.util.HashUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
public class ConsentimientoService {

    private final ConsentimientoRepository consentimientoRepository;
    private final UsuarioService usuarioService;
    private final AuditLogger auditLogger;

    public ConsentimientoService(ConsentimientoRepository consentimientoRepository,
                                 UsuarioService usuarioService,
                                 AuditLogger auditLogger) {
        this.consentimientoRepository = consentimientoRepository;
        this.usuarioService = usuarioService;
        this.auditLogger = auditLogger;
    }

    public List<Consentimiento> registrarConsentimiento(UUID usuarioId, ConsentimientoRequest request, HttpServletRequest httpRequest) {
        Usuario usuario = usuarioService.buscarPorId(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        String ip = getClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        List<Consentimiento> registros = new ArrayList<>();

        for (Map.Entry<String, Boolean> entry : request.finalidades().entrySet()) {
            if (!Boolean.TRUE.equals(entry.getValue())) continue;

            Finalidad finalidad;
            try {
                finalidad = Finalidad.valueOf(entry.getKey());
            } catch (IllegalArgumentException e) {
                continue;
            }

            String contenido = String.format("%s|%s|%s|%s|%s",
                    usuarioId, finalidad, request.versionPolitica(), request.idioma(), LocalDateTime.now());
            String hashSha256 = HashUtils.sha256(contenido);

            Consentimiento c = Consentimiento.builder()
                    .usuario(usuario)
                    .ipOrigen(ip)
                    .userAgent(userAgent)
                    .hashDocumento(hashSha256)
                    .versionPolitica(request.versionPolitica())
                    .idioma(request.idioma())
                    .finalidad(finalidad)
                    .activo(true)
                    .aceptado(true)
                    .hashSha256(hashSha256)
                    .build();

            registros.add(consentimientoRepository.save(c));
        }

        boolean tieneCuenta = request.finalidades().getOrDefault("CUENTA", false);
        usuarioService.actualizarConsentimiento(usuarioId, tieneCuenta);

        auditLogger.registrarEvento(
                "CONSENTIMIENTO_MULTIPLE",
                usuarioId,
                "REGISTRAR",
                "CONSENTIMIENTO",
                usuarioId,
                Map.of("finalidades", String.join(",", request.finalidades().keySet()))
        );

        return registros;
    }

    public void revocarConsentimiento(UUID usuarioId, String finalidadStr) {
        Finalidad finalidad;
        try {
            finalidad = Finalidad.valueOf(finalidadStr);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Finalidad inválida: " + finalidadStr);
        }

        List<Consentimiento> activos = consentimientoRepository
                .findByUsuarioIdAndFinalidadAndActivoTrue(usuarioId, finalidad);

        for (Consentimiento c : activos) {
            c.setActivo(false);
            c.setFechaRevocacion(LocalDateTime.now());
            consentimientoRepository.save(c);
        }

        auditLogger.registrarEvento(
                "CONSENTIMIENTO_REVOCADO",
                usuarioId,
                "REVOCAR",
                "CONSENTIMIENTO",
                usuarioId,
                Map.of("finalidad", finalidadStr)
        );
    }

    public boolean haOtorgadoConsentimiento(UUID usuarioId, String finalidadStr) {
        Finalidad finalidad;
        try {
            finalidad = Finalidad.valueOf(finalidadStr);
        } catch (IllegalArgumentException e) {
            return false;
        }
        return !consentimientoRepository.findByUsuarioIdAndFinalidadAndActivoTrue(usuarioId, finalidad).isEmpty();
    }

    public Map<String, Boolean> obtenerEstadoConsentimientos(UUID usuarioId) {
        Map<String, Boolean> estado = new java.util.LinkedHashMap<>();
        for (Finalidad f : Finalidad.values()) {
            estado.put(f.name(), haOtorgadoConsentimiento(usuarioId, f.name()));
        }
        return estado;
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip != null ? ip.substring(0, Math.min(ip.length(), 45)) : "0.0.0.0";
    }
}
