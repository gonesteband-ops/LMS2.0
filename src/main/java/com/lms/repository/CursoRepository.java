package com.lms.repository;

import com.lms.model.Curso;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CursoRepository extends JpaRepository<Curso, UUID> {
    Page<Curso> findByDeletedAtIsNull(Pageable pageable);
    Optional<Curso> findByIdAndDeletedAtIsNull(UUID id);
    Page<Curso> findByDeletedAtIsNullAndTituloContainingIgnoreCase(String titulo, Pageable pageable);
    long countByDeletedAtIsNull();
}
