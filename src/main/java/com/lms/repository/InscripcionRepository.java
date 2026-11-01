package com.lms.repository;

import com.lms.model.Curso;
import com.lms.model.Inscripcion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio de inscripciones (participación de usuarios en cursos).
 */
@Repository
public interface InscripcionRepository extends JpaRepository<Inscripcion, UUID> {

    List<Inscripcion> findByUsuarioIdAndDeletedAtIsNull(UUID usuarioId);

    List<Inscripcion> findByCursoIdAndDeletedAtIsNull(UUID cursoId);

    Optional<Inscripcion> findByUsuarioIdAndCursoIdAndDeletedAtIsNull(UUID usuarioId, UUID cursoId);

    long countByUsuarioIdAndCompletadoTrueAndDeletedAtIsNull(UUID usuarioId);

    long countByUsuarioIdAndCompletadoFalseAndDeletedAtIsNull(UUID usuarioId);

    /**
     * Cursos del usuario que están pendientes y cuya fecha límite no ha expirado.
     */
    @Query("SELECT i FROM Inscripcion i WHERE i.usuario.id = :usuarioId " +
           "AND i.completado = false AND i.deletedAt IS NULL " +
           "ORDER BY i.fechaLimite ASC")
    List<Inscripcion> findPendientesByUsuarioId(@Param("usuarioId") UUID usuarioId);

    /**
     * Cursos del usuario que están completados.
     */
    @Query("SELECT i FROM Inscripcion i WHERE i.usuario.id = :usuarioId " +
           "AND i.completado = true AND i.deletedAt IS NULL " +
           "ORDER BY i.fechaCompletado DESC")
    List<Inscripcion> findCompletadosByUsuarioId(@Param("usuarioId") UUID usuarioId);

    /**
     * Cursos próximos a vencer (dentro de los próximos N días).
     */
    @Query("SELECT i FROM Inscripcion i WHERE i.usuario.id = :usuarioId " +
           "AND i.completado = false AND i.deletedAt IS NULL " +
           "AND i.fechaLimite IS NOT NULL " +
           "AND i.fechaLimite BETWEEN :inicio AND :fin " +
           "ORDER BY i.fechaLimite ASC")
    List<Inscripcion> findProximosAVencer(
            @Param("usuarioId") UUID usuarioId,
            @Param("inicio") LocalDateTime inicio,
            @Param("fin") LocalDateTime fin);

    boolean existsByUsuarioIdAndCursoIdAndDeletedAtIsNull(UUID usuarioId, UUID cursoId);

    long countByCursoIdAndDeletedAtIsNull(UUID cursoId);

    long countByCursoIdAndCompletadoTrueAndDeletedAtIsNull(UUID cursoId);

    long countByUsuarioIdAndDeletedAtIsNull(UUID usuarioId);

    /**
     * Retorna los cursos en los que un usuario está inscrito (activos).
     */
    @Query("SELECT i.curso FROM Inscripcion i WHERE i.usuario.id = :usuarioId AND i.deletedAt IS NULL AND i.curso.deletedAt IS NULL")
    List<Curso> findCursosInscritosByUsuarioId(@Param("usuarioId") UUID usuarioId);

    @Query("SELECT MAX(i.updatedAt) FROM Inscripcion i WHERE i.curso.id = :cursoId AND i.deletedAt IS NULL")
    Optional<LocalDateTime> findUltimoAccesoByCursoId(@Param("cursoId") UUID cursoId);

    @Query("SELECT i.usuario FROM Inscripcion i WHERE i.curso.id = :cursoId AND i.deletedAt IS NULL AND i.usuario.deletedAt IS NULL")
    List<com.lms.model.Usuario> findUsuariosByCursoId(@Param("cursoId") UUID cursoId);
}
