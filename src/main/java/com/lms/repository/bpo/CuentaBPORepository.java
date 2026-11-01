package com.lms.repository.bpo;

import com.lms.model.bpo.CuentaBPO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CuentaBPORepository extends JpaRepository<CuentaBPO, UUID> {

    Optional<CuentaBPO> findByCodigo(String codigo);

    Optional<CuentaBPO> findByNombre(String nombre);
}
