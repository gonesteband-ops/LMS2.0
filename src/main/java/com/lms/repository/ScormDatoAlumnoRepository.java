package com.lms.repository;

import com.lms.model.ScormDatoAlumno;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ScormDatoAlumnoRepository extends JpaRepository<ScormDatoAlumno, UUID> {

    List<ScormDatoAlumno> findByPaqueteIdAndUsuarioId(UUID paqueteId, UUID usuarioId);

    Optional<ScormDatoAlumno> findByPaqueteIdAndUsuarioIdAndIntentoIdAndClave(
            UUID paqueteId, UUID usuarioId, UUID intentoId, String clave);

    void deleteByPaqueteIdAndUsuarioIdAndIntentoId(UUID paqueteId, UUID usuarioId, UUID intentoId);

    void deleteByPaqueteId(UUID paqueteId);

    @Query("SELECT DISTINCT s.usuarioId FROM ScormDatoAlumno s WHERE s.paqueteId = :paqueteId")
    List<UUID> findDistinctUsuarioIdByPaqueteId(@Param("paqueteId") UUID paqueteId);

    @Query("SELECT s FROM ScormDatoAlumno s WHERE s.paqueteId = :paqueteId AND s.usuarioId = :usuarioId " +
           "AND s.clave IN ('cmi.core.lesson_status', 'cmi.completion_status', 'cmi.core.score.raw', 'cmi.core.session_time')")
    List<ScormDatoAlumno> findProgresoByPaqueteAndUsuario(
            @Param("paqueteId") UUID paqueteId,
            @Param("usuarioId") UUID usuarioId);
}
