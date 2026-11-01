package com.lms.repository;

import com.lms.model.CursoDocumento;
import com.lms.model.CursoDocumentoId;
import com.lms.model.Documento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repositorio de la relación N:M entre Curso y Documento.
 */
@Repository
public interface CursoDocumentoRepository extends JpaRepository<CursoDocumento, CursoDocumentoId> {

    List<CursoDocumento> findByCursoId(UUID cursoId);

    @Query("SELECT cd FROM CursoDocumento cd JOIN FETCH cd.documento WHERE cd.curso.id = :cursoId")
    List<CursoDocumento> findDocumentosByCursoId(@Param("cursoId") UUID cursoId);

    List<CursoDocumento> findByDocumentoId(UUID documentoId);

    void deleteByCursoIdAndDocumentoId(UUID cursoId, UUID documentoId);

    boolean existsByCursoIdAndDocumentoId(UUID cursoId, UUID documentoId);

    long countByCursoId(UUID cursoId);
}
