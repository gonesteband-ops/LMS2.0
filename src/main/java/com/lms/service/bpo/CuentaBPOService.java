package com.lms.service.bpo;

import com.lms.model.Usuario;
import com.lms.model.bpo.CuentaBPO;
import com.lms.model.bpo.SubServicioBPO;
import com.lms.model.bpo.UsuarioCuentaBPO;
import com.lms.model.bpo.UsuarioCuentaBPOId;
import com.lms.repository.UsuarioRepository;
import com.lms.repository.bpo.CuentaBPORepository;
import com.lms.repository.bpo.SubServicioBPORepository;
import com.lms.repository.bpo.UsuarioCuentaBPORepository;
import com.lms.service.CodigoGeneratorService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class CuentaBPOService {

    private final CuentaBPORepository cuentaRepository;
    private final SubServicioBPORepository subservicioRepository;
    private final UsuarioCuentaBPORepository usuarioCuentaRepository;
    private final CodigoGeneratorService codigoGeneratorService;
    private final UsuarioRepository usuarioRepository;

    public CuentaBPOService(CuentaBPORepository cuentaRepository,
                            SubServicioBPORepository subservicioRepository,
                            UsuarioCuentaBPORepository usuarioCuentaRepository,
                            CodigoGeneratorService codigoGeneratorService,
                            UsuarioRepository usuarioRepository) {
        this.cuentaRepository = cuentaRepository;
        this.subservicioRepository = subservicioRepository;
        this.usuarioCuentaRepository = usuarioCuentaRepository;
        this.codigoGeneratorService = codigoGeneratorService;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional(readOnly = true)
    public List<UUID> obtenerCuentaIdsPorUsuario(UUID usuarioId) {
        return usuarioCuentaRepository.findCuentaIdsByUsuarioId(usuarioId);
    }

    @Transactional(readOnly = true)
    public List<CuentaBPO> obtenerCuentasActivas() {
        return cuentaRepository.findAll().stream()
                .filter(CuentaBPO::isActiva)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CuentaBPO> obtenerTodasLasCuentas() {
        return cuentaRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<CuentaBPO> buscarPorCodigo(String codigo) {
        return cuentaRepository.findByCodigo(codigo);
    }

    @Transactional(readOnly = true)
    public Optional<CuentaBPO> buscarPorId(UUID id) {
        return cuentaRepository.findById(id);
    }

    public CuentaBPO crearCuenta(CuentaBPO gestor, UUID creadorId) {
        if (gestor.getNombre() != null && cuentaRepository.findByNombre(gestor.getNombre()).isPresent()) {
            throw new RuntimeException("El nombre '" + gestor.getNombre() + "' ya está en uso");
        }
        gestor.setCodigo(codigoGeneratorService.generarCodigo("GES"));
        CuentaBPO guardado = cuentaRepository.save(gestor);

        // Asignar el creador a la cuenta para que tenga permisos
        Usuario usuario = usuarioRepository.findById(creadorId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        UsuarioCuentaBPO asignacion = UsuarioCuentaBPO.builder()
                .id(new UsuarioCuentaBPOId(creadorId, guardado.getId()))
                .usuario(usuario)
                .cuenta(guardado)
                .createdBy(creadorId)
                .build();
        usuarioCuentaRepository.save(asignacion);

        return guardado;
    }

    public CuentaBPO actualizarCuenta(UUID id, String nombre, String codigo, boolean activa) {
        CuentaBPO gestor = cuentaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Gestor no encontrado"));
        if (!gestor.getCodigo().equals(codigo) && cuentaRepository.findByCodigo(codigo).isPresent()) {
            throw new RuntimeException("El código '" + codigo + "' ya está en uso");
        }
        if (!gestor.getNombre().equals(nombre) && cuentaRepository.findByNombre(nombre).isPresent()) {
            throw new RuntimeException("El nombre '" + nombre + "' ya está en uso");
        }
        gestor.setNombre(nombre);
        gestor.setCodigo(codigo);
        gestor.setActiva(activa);
        gestor.setUpdatedAt(LocalDateTime.now());
        return cuentaRepository.save(gestor);
    }

    public CuentaBPO toggleActiva(UUID id) {
        CuentaBPO gestor = cuentaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Gestor no encontrado: " + id));
        gestor.setActiva(!gestor.isActiva());
        gestor.setUpdatedAt(LocalDateTime.now());
        return cuentaRepository.save(gestor);
    }

    public void eliminarCuenta(UUID id, UUID usuarioId, String motivo) {
        CuentaBPO gestor = cuentaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Gestor no encontrado"));
        gestor.setDeletedAt(LocalDateTime.now());
        cuentaRepository.save(gestor);
    }

    public SubServicioBPO crearSubcarpeta(UUID cuentaId, String nombre) {
        var cuenta = cuentaRepository.findById(cuentaId)
                .orElseThrow(() -> new RuntimeException("Cuenta no encontrada"));
        String codigo = java.text.Normalizer.normalize(nombre, java.text.Normalizer.Form.NFD)
                .replaceAll("[^\\p{ASCII}]", "")
                .toLowerCase()
                .replaceAll("[^a-zA-Z0-9]+", "-")
                .replaceAll("^-|-$", "");
        if (codigo.isEmpty()) codigo = "subcarpeta";
        // Asegurar unicidad del codigo dentro de la cuenta
        String codigoBase = codigo;
        int sufijo = 1;
        while (subservicioRepository.findByCuentaCodigoAndCodigo(cuenta.getCodigo(), codigo).isPresent()) {
            codigo = codigoBase + "-" + sufijo++;
        }
        SubServicioBPO sub = SubServicioBPO.builder()
                .nombre(nombre)
                .codigo(codigo)
                .cuenta(cuenta)
                .activo(true)
                .build();
        return subservicioRepository.save(sub);
    }

    public SubServicioBPO actualizarSubcarpeta(UUID subId, String nombre, UUID usuarioId) {
        SubServicioBPO sub = subservicioRepository.findById(subId)
                .orElseThrow(() -> new RuntimeException("Subcarpeta no encontrada"));
        sub.setNombre(nombre);
        sub.setUpdatedAt(LocalDateTime.now());
        return subservicioRepository.save(sub);
    }

    public void eliminarSubcarpeta(UUID subId, UUID usuarioId, String motivo) {
        SubServicioBPO sub = subservicioRepository.findById(subId)
                .orElseThrow(() -> new RuntimeException("Subcarpeta no encontrada"));
        sub.setDeletedAt(LocalDateTime.now());
        subservicioRepository.save(sub);
    }

    @Transactional(readOnly = true)
    public Optional<SubServicioBPO> obtenerSubservicioPorCuentaCodigo(String cuentaCodigo, String subCodigo) {
        return subservicioRepository.findByCuentaCodigoAndCodigo(cuentaCodigo, subCodigo);
    }

    @Transactional(readOnly = true)
    public List<SubServicioBPO> obtenerSubserviciosPorCuentaCodigo(String cuentaCodigo) {
        return subservicioRepository.findByCuentaCodigo(cuentaCodigo);
    }

    @Transactional(readOnly = true)
    public List<SubServicioBPO> obtenerSubserviciosPorCuenta(UUID cuentaId) {
        return subservicioRepository.findByCuentaIdAndActivoTrue(cuentaId);
    }

    @Transactional(readOnly = true)
    public List<UUID> obtenerSubservicioIdsPorUsuarioYCuenta(UUID usuarioId, UUID cuentaId) {
        return usuarioCuentaRepository.findSubservicioIdsByUsuarioIdAndCuentaId(usuarioId, cuentaId);
    }

    public void asignarUsuarioACuenta(UUID usuarioId, UUID cuentaId, UUID creadorId) {
        if (usuarioCuentaRepository.findById(new UsuarioCuentaBPOId(usuarioId, cuentaId)).isPresent()) {
            return; // ya asignado
        }
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        CuentaBPO cuenta = cuentaRepository.findById(cuentaId)
                .orElseThrow(() -> new RuntimeException("Cuenta no encontrada"));
        UsuarioCuentaBPO asignacion = UsuarioCuentaBPO.builder()
                .id(new UsuarioCuentaBPOId(usuarioId, cuentaId))
                .usuario(usuario)
                .cuenta(cuenta)
                .createdBy(creadorId)
                .build();
        usuarioCuentaRepository.save(asignacion);
    }
}
