package com.lms.repository;

import com.lms.model.IncidenteSeguridad;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface IncidenteSeguridadRepository extends JpaRepository<IncidenteSeguridad, UUID> {

    List<IncidenteSeguridad> findByEstadoOrderByFechaDeteccionDesc(String estado);

    List<IncidenteSeguridad> findAllByOrderByFechaDeteccionDesc();

    long countByFechaDeteccionAfterAndNivelRiesgo(LocalDateTime since, String nivelRiesgo);
}
