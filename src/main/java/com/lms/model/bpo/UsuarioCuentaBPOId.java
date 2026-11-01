package com.lms.model.bpo;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;
import java.util.UUID;

@Embeddable
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @EqualsAndHashCode
public class UsuarioCuentaBPOId implements Serializable {

    @Column(name = "usuario_id", nullable = false)
    private UUID usuarioId;

    @Column(name = "cuenta_id", nullable = false)
    private UUID cuentaId;
}
