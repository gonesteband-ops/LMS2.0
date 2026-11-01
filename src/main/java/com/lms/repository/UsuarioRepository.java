package com.lms.repository;

import com.lms.model.Usuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {

    Optional<Usuario> findByEmailAndDeletedAtIsNull(String email);

    Optional<Usuario> findByEmail(String email);

    Optional<Usuario> findByRut(String rut);

    Optional<Usuario> findByRutAndDeletedAtIsNull(String rut);

    boolean existsByEmail(String email);

    @Query("SELECT u FROM Usuario u WHERE u.email = ?1 AND u.deletedAt IS NULL AND u.consentimientoOtorgado = true")
    Optional<Usuario> findActiveWithConsentimiento(String email);

    @Query("SELECT u FROM Usuario u WHERE u.deletedAt IS NULL ORDER BY u.createdAt DESC")
    Page<Usuario> findAllActive(Pageable pageable);

    @Query("SELECT u FROM Usuario u WHERE u.estadoTratamiento = 'PENDIENTE_SUPRESION' AND u.deletedAt > :now ORDER BY u.deletedAt ASC")
    Page<Usuario> findEnProcesoEliminacion(@Param("now") LocalDateTime now, Pageable pageable);

    @Query("SELECT u FROM Usuario u WHERE u.estadoTratamiento = 'ANONIMIZADO' OR (u.deletedAt IS NOT NULL AND u.deletedAt <= :now AND u.estadoTratamiento != 'PENDIENTE_SUPRESION') ORDER BY u.deletedAt DESC")
    Page<Usuario> findEliminados(@Param("now") LocalDateTime now, Pageable pageable);
}
