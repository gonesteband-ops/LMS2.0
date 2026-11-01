package com.lms.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lms.model.Consentimiento;
import com.lms.model.FirmaElectronica;
import com.lms.model.Inscripcion;
import com.lms.model.Usuario;
import com.lms.repository.ConsentimientoRepository;
import com.lms.repository.FirmaElectronicaRepository;
import com.lms.repository.InscripcionRepository;
import com.lms.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@Transactional
public class ArcopService {

    private final UsuarioRepository usuarioRepository;
    private final InscripcionRepository inscripcionRepository;
    private final ConsentimientoRepository consentimientoRepository;
    private final FirmaElectronicaRepository firmaRepository;
    private final ObjectMapper objectMapper;

    public ArcopService(UsuarioRepository usuarioRepository,
                        InscripcionRepository inscripcionRepository,
                        ConsentimientoRepository consentimientoRepository,
                        FirmaElectronicaRepository firmaRepository,
                        ObjectMapper objectMapper) {
        this.usuarioRepository = usuarioRepository;
        this.inscripcionRepository = inscripcionRepository;
        this.consentimientoRepository = consentimientoRepository;
        this.firmaRepository = firmaRepository;
        this.objectMapper = objectMapper;
    }

    public Map<String, Object> obtenerAcceso(UUID usuarioId) {
        Usuario u = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        List<Inscripcion> inscripciones = inscripcionRepository.findByUsuarioIdAndDeletedAtIsNull(usuarioId);
        List<Consentimiento> consents = consentimientoRepository.findByUsuarioIdOrderByCreatedAtDesc(usuarioId);
        List<FirmaElectronica> firmas = firmaRepository.findByUsuarioId(usuarioId);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("datosPersonales", Map.of(
                "nombre", u.getNombreCompleto(),
                "email", u.getEmail(),
                "rut", u.getRut() != null ? u.getRut() : "No proporcionado",
                "fechaNacimiento", u.getFechaNacimiento() != null ? u.getFechaNacimiento().toString() : "No proporcionada",
                "fechaRegistro", u.getCreatedAt() != null ? u.getCreatedAt().toString() : null
        ));
        response.put("cursosInscritos", inscripciones.size());
        response.put("cursosCompletados", inscripciones.stream().filter(Inscripcion::isCompletado).count());
        response.put("consentimientos", consents.stream().map(c -> Map.of(
                "finalidad", c.getFinalidad() != null ? c.getFinalidad().name() : "LEGACY",
                "activo", c.isActivo(),
                "fecha", c.getCreatedAt() != null ? c.getCreatedAt().toString() : null
        )).toList());
        response.put("firmas", firmas.stream().map(f -> Map.of(
                "fecha", f.getCreatedAt() != null ? f.getCreatedAt().toString() : null,
                "hash", f.getHashSha256()
        )).toList());
        return response;
    }

    public void rectificar(UUID usuarioId, String nombre, String email, String rut, LocalDate fechaNacimiento) {
        Usuario u = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        if (nombre != null && !nombre.isBlank()) u.setNombreCompleto(nombre.trim());
        if (email != null && !email.isBlank()) {
            if (!email.equals(u.getEmail()) && usuarioRepository.existsByEmail(email)) {
                throw new IllegalArgumentException("El email ya estÃ¡ en uso");
            }
            u.setEmail(email.toLowerCase().trim());
        }
        if (rut != null && !rut.isBlank()) u.setRut(rut.trim());
        if (fechaNacimiento != null) u.setFechaNacimiento(fechaNacimiento);

        usuarioRepository.save(u);
    }

    public void solicitarSupresion(UUID usuarioId, String motivo) {
        Usuario u = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        u.setDeletedAt(LocalDateTime.now().plusDays(30));
        u.setEstadoTratamiento("PENDIENTE_SUPRESION");
        u.setReason(motivo);
        usuarioRepository.save(u);
    }

    public void cancelarSupresion(UUID usuarioId) {
        Usuario u = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        u.setDeletedAt(null);
        u.setEstadoTratamiento("ACTIVO");
        u.setReason(null);
        u.setBloqueadoHasta(null);
        usuarioRepository.save(u);
    }

    public void bloquear(UUID usuarioId) {
        Usuario u = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
                u.setBloqueadoHasta(LocalDateTime.now().plusDays(90));
        u.setEstadoTratamiento("BLOQUEADO");
        usuarioRepository.save(u);
    }

    public void desbloquear(UUID usuarioId) {
        Usuario u = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        u.setBloqueadoHasta(null);
        usuarioRepository.save(u);
    }

    public String portabilidadJson(UUID usuarioId) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(obtenerAcceso(usuarioId));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error generando archivo de portabilidad", e);
        }
    }
}


