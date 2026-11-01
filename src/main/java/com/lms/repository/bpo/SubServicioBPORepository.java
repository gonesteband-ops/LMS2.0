package com.lms.repository.bpo;

import com.lms.model.bpo.SubServicioBPO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SubServicioBPORepository extends JpaRepository<SubServicioBPO, UUID> {

    List<SubServicioBPO> findByCuentaId(UUID cuentaId);

    List<SubServicioBPO> findByCuentaIdAndActivoTrue(UUID cuentaId);

    Optional<SubServicioBPO> findByCuentaCodigoAndCodigo(String cuentaCodigo, String codigo);

    List<SubServicioBPO> findByCuentaCodigo(String cuentaCodigo);
}
