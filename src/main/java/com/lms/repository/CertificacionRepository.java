package com.lms.repository;

import com.lms.model.Certificacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CertificacionRepository extends JpaRepository<Certificacion, UUID> {
    Optional<Certificacion> findByCodigo(String codigo);
    List<Certificacion> findByDeletedAtIsNull();
    List<Certificacion> findByActivoTrueAndDeletedAtIsNull();
}
