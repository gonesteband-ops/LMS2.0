package com.lms.repository;

import com.lms.model.PreguntaCertificacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PreguntaCertificacionRepository extends JpaRepository<PreguntaCertificacion, UUID> {
    List<PreguntaCertificacion> findByCertificacionIdOrderByOrdenAsc(UUID certificacionId);
}
