package com.lms.repository;

import com.lms.model.Consentimiento;
import com.lms.model.Consentimiento.Finalidad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ConsentimientoRepository extends JpaRepository<Consentimiento, UUID> {
    List<Consentimiento> findByUsuarioIdOrderByCreatedAtDesc(UUID usuarioId);
    List<Consentimiento> findByUsuarioIdAndFinalidadAndActivoTrue(UUID usuarioId, Finalidad finalidad);
}
