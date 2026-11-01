package com.lms.repository;

import com.lms.model.PaqueteScorm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaqueteScormRepository extends JpaRepository<PaqueteScorm, UUID> {

    List<PaqueteScorm> findByCursoIdAndDeletedAtIsNullOrderByCreatedAtDesc(UUID cursoId);

    Optional<PaqueteScorm> findByIdAndDeletedAtIsNull(UUID id);

    Optional<PaqueteScorm> findByCodigoAndDeletedAtIsNull(String codigo);

    long countByCursoIdAndDeletedAtIsNull(UUID cursoId);
}
