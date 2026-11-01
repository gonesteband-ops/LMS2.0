package com.lms.repository.bpo;

import com.lms.model.bpo.UsuarioCuentaBPO;
import com.lms.model.bpo.UsuarioCuentaBPOId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface UsuarioCuentaBPORepository extends JpaRepository<UsuarioCuentaBPO, UsuarioCuentaBPOId> {

    @Query("SELECT uc.cuenta.id FROM UsuarioCuentaBPO uc WHERE uc.usuario.id = :usuarioId")
    List<UUID> findCuentaIdsByUsuarioId(@Param("usuarioId") UUID usuarioId);

    @Query("SELECT uc.subservicio.id FROM UsuarioCuentaBPO uc WHERE uc.usuario.id = :usuarioId AND uc.cuenta.id = :cuentaId")
    List<UUID> findSubservicioIdsByUsuarioIdAndCuentaId(@Param("usuarioId") UUID usuarioId, @Param("cuentaId") UUID cuentaId);

    List<UsuarioCuentaBPO> findByCuentaId(@Param("cuentaId") UUID cuentaId);
}
