package com.lms.service;

import com.lms.model.IncidenteSeguridad;
import com.lms.repository.IncidenteSeguridadRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
public class IncidenteSeguridadService {

    private final IncidenteSeguridadRepository repository;

    public IncidenteSeguridadService(IncidenteSeguridadRepository repository) {
        this.repository = repository;
    }

    public List<IncidenteSeguridad> listar(String estado) {
        if (estado != null && !estado.isBlank()) {
            return repository.findByEstadoOrderByFechaDeteccionDesc(estado);
        }
        return repository.findAllByOrderByFechaDeteccionDesc();
    }

    public IncidenteSeguridad registrar(Map<String, Object> data) {
        String contenido = data.toString() + "|" + LocalDateTime.now();
        String hash = com.lms.util.HashUtils.sha256(contenido);

        IncidenteSeguridad incidente = IncidenteSeguridad.builder()
                .tipo((String) data.getOrDefault("tipo", "OTRO"))
                .nivelRiesgo((String) data.getOrDefault("nivelRiesgo", "BAJO"))
                .descripcion((String) data.getOrDefault("descripcion", ""))
                .datosAfectados((String) data.get("datosAfectados"))
                .usuariosAfectados((Integer) data.get("usuariosAfectados"))
                .ipOrigen((String) data.get("ipOrigen"))
                .fechaDeteccion(LocalDateTime.now())
                .estado("DETECTADO")
                .hashSha256(hash)
                .createdAt(LocalDateTime.now())
                .build();

        return repository.save(incidente);
    }

    public IncidenteSeguridad actualizarEstado(UUID id, String nuevoEstado) {
        IncidenteSeguridad incidente = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Incidente no encontrado"));
        incidente.setEstado(nuevoEstado);
        if ("NOTIFICADO".equals(nuevoEstado)) {
            incidente.setFechaNotificacionAgencia(LocalDateTime.now());
        }
        return repository.save(incidente);
    }
}
