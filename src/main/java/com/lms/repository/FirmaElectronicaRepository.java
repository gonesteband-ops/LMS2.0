package com.lms.repository;

import com.lms.model.FirmaElectronica;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface FirmaElectronicaRepository extends JpaRepository<FirmaElectronica, UUID> {

    @Query("SELECT f.hashSha256 FROM FirmaElectronica f ORDER BY f.timestamp DESC LIMIT 1")
    Optional<String> findUltimoHash();

    long countByUsuarioId(UUID usuarioId);

    boolean existsByUsuarioIdAndDocumentoId(UUID usuarioId, UUID documentoId);

    java.util.List<com.lms.model.FirmaElectronica> findByUsuarioId(UUID usuarioId);
}