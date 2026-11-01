package com.lms.repository;

import com.lms.model.CertificacionUsuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CertificacionUsuarioRepository extends JpaRepository<CertificacionUsuario, UUID> {
    List<CertificacionUsuario> findByCertificacionId(UUID certificacionId);
    Optional<CertificacionUsuario> findByCertificacionIdAndUsuarioId(UUID certificacionId, UUID usuarioId);
    List<CertificacionUsuario> findByUsuarioId(UUID usuarioId);
}
