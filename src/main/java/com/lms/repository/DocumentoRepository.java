package com.lms.repository;

import com.lms.model.Documento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DocumentoRepository extends JpaRepository<Documento, UUID> {
    java.util.Optional<Documento> findByCodigo(String codigo);
    long countByDeletedAtIsNull();

    /**
     * Retorna los documentos vinculados a cursos en los que el usuario está inscrito.
     */
    @Query("SELECT DISTINCT d FROM Documento d " +
           "JOIN CursoDocumento cd ON cd.documento.id = d.id " +
           "JOIN Inscripcion i ON i.curso.id = cd.curso.id " +
           "WHERE i.usuario.id = :usuarioId AND d.deletedAt IS NULL")
    List<Documento> findByUsuarioInscripciones(@Param("usuarioId") UUID usuarioId);
}
