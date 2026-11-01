package com.lms.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Entidad Usuario - raíz de identidad del sistema.
 * <p>
 * Cumplimiento legal:
 * - UUID como PK (prohibido RUT)
 * - Control de intentos fallidos (brute-force protection)
 * - MFA obligatorio para administradores
 */
@Entity
@Table(name = "usuarios", indexes = {
    @Index(name = "idx_usuario_email", columnList = "email", unique = true)
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Usuario {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "id", updatable = false, nullable = false, columnDefinition = "UUID")
    private UUID id;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(unique = true, length = 20)
    private String rut;

    @Column(name = "nombre_completo", nullable = false, length = 255)
    private String nombreCompleto;

    @Column(name = "hash_contrasena", nullable = false, length = 128)
    private String hashContrasena;

    @Column(name = "mfa_secreto", length = 64)
    private String mfaSecreto;

    @Column(name = "mfa_habilitado", nullable = false)
    private boolean mfaHabilitado = false;

    @Column(name = "ultimo_login")
    private LocalDateTime ultimoLogin;

    @Column(name = "intentos_fallidos", nullable = false)
    private int intentosFallidos = 0;

    @Column(name = "bloqueado_hasta")
    private LocalDateTime bloqueadoHasta;

    @Column(name = "hash_sha256", nullable = false, length = 64)
    private String hashSha256;

    @Column(name = "fecha_nacimiento")
    private LocalDate fechaNacimiento;

    @Column(name = "consentimiento_otorgado", nullable = false)
    private boolean consentimientoOtorgado = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "deleted_by")
    private UUID deletedBy;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Builder.Default
    @Column(name = "estado_tratamiento", nullable = false, length = 20, columnDefinition = "VARCHAR(20) NOT NULL DEFAULT 'ACTIVO'")
    private String estadoTratamiento = "ACTIVO";

    // Relaciones
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "usuarios_roles",
        joinColumns = @JoinColumn(name = "usuario_id"),
        inverseJoinColumns = @JoinColumn(name = "rol_id")
    )
    private Set<Rol> roles = new HashSet<>();

    @OneToMany(mappedBy = "usuario", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<Consentimiento> consentimientos = new HashSet<>();

    @OneToMany(mappedBy = "usuario", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<FirmaElectronica> firmas = new HashSet<>();

    @OneToMany(mappedBy = "usuario", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Inscripcion> inscripciones = new ArrayList<>();

    public boolean isActive() {
        return deletedAt == null;
    }

    public boolean isBloqueado() {
        return bloqueadoHasta != null && bloqueadoHasta.isAfter(LocalDateTime.now());
    }

    @Transient
    public String getEstado() {
        if ("ANONIMIZADO".equals(estadoTratamiento)) return "Eliminado";
        if (deletedAt != null) {
            if ("PENDIENTE_SUPRESION".equals(estadoTratamiento) && deletedAt.isAfter(LocalDateTime.now())) {
                return "Inactivo";
            }
            return "Eliminado";
        }
        if ("BLOQUEADO".equals(estadoTratamiento) && bloqueadoHasta != null && bloqueadoHasta.isAfter(LocalDateTime.now())) {
            return "Bloqueado";
        }
        return "Activo";
    }
}
