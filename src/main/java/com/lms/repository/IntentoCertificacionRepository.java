package com.lms.repository;

import com.lms.model.IntentoCertificacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IntentoCertificacionRepository extends JpaRepository<IntentoCertificacion, UUID> {
    List<IntentoCertificacion> findByCertificacionIdAndUsuarioIdAndDeletedAtIsNull(UUID certificacionId, UUID usuarioId);
    Optional<IntentoCertificacion> findByCertificacionIdAndUsuarioIdAndEstado(UUID certificacionId, UUID usuarioId, String estado);
}
