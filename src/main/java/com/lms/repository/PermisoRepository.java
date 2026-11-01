package com.lms.repository;

import com.lms.model.Permiso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PermisoRepository extends JpaRepository<Permiso, UUID> {
    Optional<Permiso> findByNombre(String nombre);
}
