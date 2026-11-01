package com.lms.model.bpo;

import com.lms.model.Usuario;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "usuarios_cuenta_bpo")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UsuarioCuentaBPO {

    @EmbeddedId
    private UsuarioCuentaBPOId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("usuarioId")
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("cuentaId")
    @JoinColumn(name = "cuenta_id", nullable = false)
    private CuentaBPO cuenta;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subservicio_id")
    private SubServicioBPO subservicio;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;
}
