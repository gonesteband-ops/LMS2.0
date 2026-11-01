package com.lms.repository;

import com.lms.model.VersionCurso;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface VersionCursoRepository extends JpaRepository<VersionCurso, UUID> {
    Page<VersionCurso> findByCursoIdOrderByNumeroVersionDesc(UUID cursoId, Pageable pageable);
    Optional<VersionCurso> findTopByCursoIdOrderByNumeroVersionDesc(UUID cursoId);
}
