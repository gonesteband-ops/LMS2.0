package com.lms.repository;

import com.lms.model.Evaluacion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EvaluacionRepository extends JpaRepository<Evaluacion, UUID> {

    @Query("SELECT e FROM Evaluacion e WHERE e.curso.id = :cursoId AND e.deletedAt IS NULL")
    List<Evaluacion> findByCursoId(@Param("cursoId") UUID cursoId);

    @Query("SELECT e FROM Evaluacion e WHERE e.curso.id = :cursoId AND e.deletedAt IS NULL")
    Page<Evaluacion> findByCursoId(@Param("cursoId") UUID cursoId, Pageable pageable);

    java.util.Optional<Evaluacion> findByCodigo(String codigo);
}
