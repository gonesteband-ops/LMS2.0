package com.lms.service;

import com.lms.dto.FirmaRequest;
import com.lms.dto.FirmaResponse;
import com.lms.model.Documento;
import com.lms.model.FirmaElectronica;
import com.lms.model.Usuario;
import com.lms.repository.DocumentoRepository;
import com.lms.repository.FirmaElectronicaRepository;
import com.lms.util.AuditLogger;
import com.lms.util.HashUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Servicio de firma electrónica (Ley 19.799).
 * <p>
 * Implementa hash encadenado para integridad de la cadena de firmas.
 */
@Service
@Transactional
public class FirmaService {

    private final FirmaElectronicaRepository firmaRepository;
    private final DocumentoRepository documentoRepository;
    private final UsuarioService usuarioService;
    private final AuditLogger auditLogger;

    public FirmaService(FirmaElectronicaRepository firmaRepository,
                        DocumentoRepository documentoRepository,
                        UsuarioService usuarioService,
                        AuditLogger auditLogger) {
        this.firmaRepository = firmaRepository;
        this.documentoRepository = documentoRepository;
        this.usuarioService = usuarioService;
        this.auditLogger = auditLogger;
    }

    /**
     * Registra una firma electrónica con hash encadenado.
     */
    public FirmaResponse firmarDocumento(UUID usuarioId, FirmaRequest request, HttpServletRequest httpRequest) {
        Usuario usuario = usuarioService.buscarPorId(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        if (!usuario.isConsentimientoOtorgado()) {
            throw new IllegalStateException("Debe otorgar consentimiento antes de firmar documentos");
        }

        Documento documento = documentoRepository.findById(request.documentoId())
                .orElseThrow(() -> new IllegalArgumentException("Documento no encontrado"));

        // Obtener último hash para encadenamiento
        String hashAnterior = firmaRepository.findUltimoHash().orElse(null);

        String ipCliente = getClientIp(httpRequest);
        String contenido = String.format("%s|%s|%s|%s",
                usuarioId, documento.getId(), LocalDateTime.now(), ipCliente);
        String hashSha256 = HashUtils.sha256Encadenado(contenido, hashAnterior);

        FirmaElectronica firma = FirmaElectronica.builder()
                .usuario(usuario)
                .documento(documento)
                .ipOrigen(ipCliente)
                .timestamp(LocalDateTime.now())
                .userAgent(httpRequest.getHeader("User-Agent"))
                .sistemaOperativo(request.sistemaOperativo())
                .geolocalizacion(request.geolocalizacion())
                .hashDocumento(documento.getHashSha256())
                .hashAnterior(hashAnterior)
                .hashSha256(hashSha256)
                .build();

        firma = firmaRepository.save(firma);

        auditLogger.registrarEvento(
                "FIRMA_REGISTRADA",
                usuarioId,
                "FIRMAR",
                "DOCUMENTO",
                documento.getId(),
                Map.of("hashSha256", hashSha256, "hashAnterior", hashAnterior != null ? hashAnterior : "GENESIS")
        );

        return new FirmaResponse(
                firma.getId(),
                usuarioId,
                documento.getId(),
                documento.getHashSha256(),
                hashSha256,
                hashAnterior,
                firma.getTimestamp()
        );
    }

    /**
     * Extrae la IP del cliente desde el HttpServletRequest,
     * respetando proxies inversos (X-Forwarded-For, X-Real-IP).
     */
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

    public boolean verificarFirma(UUID usuarioId, UUID documentoId) {
        return firmaRepository.existsByUsuarioIdAndDocumentoId(usuarioId, documentoId);
    }

    public java.util.List<com.lms.dto.FirmaHistorialResponse> listarPorUsuario(UUID usuarioId) {
        return firmaRepository.findByUsuarioId(usuarioId).stream()
                .map(f -> new com.lms.dto.FirmaHistorialResponse(
                    f.getId(),
                    f.getDocumento() != null ? f.getDocumento().getCodigo() : "N/A",
                    f.getDocumento() != null ? f.getDocumento().getNombreOriginal() : "N/A",
                    f.getTimestamp(),
                    f.getIpOrigen(),
                    f.getHashSha256()
                )).toList();
    }

}