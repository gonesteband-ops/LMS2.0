package com.lms.service.bpo;

import com.lms.dto.bpo.ContenidoBPOCreateDTO;
import com.lms.dto.bpo.ContenidoBPOGroupedDTO;
import com.lms.dto.bpo.ContenidoBPOResponseDTO;
import com.lms.exception.bpo.BPOContenidoNoEncontradoException;
import com.lms.exception.bpo.BPOCuentaNoAsignadaException;
import com.lms.exception.bpo.BPOEstadoInvalidoException;
import com.lms.mapper.bpo.ContenidoBPOMapper;
import com.lms.model.bpo.ContenidoBPO;
import com.lms.model.bpo.CuentaBPO;
import com.lms.model.bpo.SubServicioBPO;
import com.lms.repository.bpo.ContenidoBPORepository;
import com.lms.service.FileUploadService;
import com.lms.util.AuditLogger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.*;

@Service
@Transactional
public class ContenidoBPOService {

    private final ContenidoBPORepository contenidoRepository;
    private final CuentaBPOService cuentaBPOService;
    private final ContenidoBPOMapper mapper;
    private final AuditLogger auditLogger;
    private final FileUploadService fileUploadService;

    public ContenidoBPOService(ContenidoBPORepository contenidoRepository,
                               CuentaBPOService cuentaBPOService,
                               ContenidoBPOMapper mapper,
                               AuditLogger auditLogger,
                               FileUploadService fileUploadService) {
        this.contenidoRepository = contenidoRepository;
        this.cuentaBPOService = cuentaBPOService;
        this.mapper = mapper;
        this.auditLogger = auditLogger;
        this.fileUploadService = fileUploadService;
    }

    @Transactional(readOnly = true)
    public List<ContenidoBPOGroupedDTO> listarAgrupado(UUID usuarioId, UUID cuentaId, String q) {
        // Primero obtener subcarpetas del usuario para esta cuenta
        List<UUID> subservicioIds = cuentaBPOService.obtenerSubservicioIdsPorUsuarioYCuenta(usuarioId, cuentaId);
        if (subservicioIds == null || subservicioIds.isEmpty() || subservicioIds.stream().allMatch(java.util.Objects::isNull)) {
            subservicioIds = cuentaBPOService.obtenerSubserviciosPorCuenta(cuentaId).stream()
                    .map(SubServicioBPO::getId).toList();
        } else {
            subservicioIds = subservicioIds.stream().filter(java.util.Objects::nonNull).toList();
        }

        List<ContenidoBPOGroupedDTO> resultado = new ArrayList<>();
        for (UUID subId : subservicioIds) {
            var subOpt = cuentaBPOService.obtenerSubserviciosPorCuenta(cuentaId).stream()
                    .filter(s -> s.getId().equals(subId)).findFirst();
            if (subOpt.isEmpty()) continue;

            List<ContenidoBPO> contenidos;
            if (q != null && !q.isBlank()) {
                contenidos = contenidoRepository.buscarPorSubservicioYTexto(subId, q);
            } else {
                contenidos = contenidoRepository.findTop5BySubservicioIdAndEstadoAndDeletedAtIsNullOrderByCreatedAtDesc(
                        subId, "publicado");
            }

            long total = contenidoRepository.countBySubservicioIdAndEstadoAndDeletedAtIsNull(subId, "publicado");
            List<ContenidoBPOResponseDTO> dtoList = contenidos.stream().map(mapper::toResponseDTO).toList();
            resultado.add(ContenidoBPOGroupedDTO.builder()
                    .subservicioNombre(subOpt.get().getNombre())
                    .subservicioId(subId)
                    .total(total)
                    .contenidos(dtoList).build());
        }
        return resultado;
    }

    @Transactional(readOnly = true)
    public List<ContenidoBPO> listarPublicacionesPorSubcarpeta(UUID subservicioId, int page, int size) {
        return contenidoRepository.findBySubservicioIdAndEstadoAndDeletedAtIsNullOrderByCreatedAtDesc(
                subservicioId, "publicado");
    }

    @Transactional(readOnly = true)
    public Optional<ContenidoBPO> buscarPorId(UUID id) {
        return contenidoRepository.findById(id);
    }

    public ContenidoBPO crearPublicacion(ContenidoBPOCreateDTO dto, UUID usuarioId, UUID cuentaId, UUID subservicioId) {
        CuentaBPO cuenta = cuentaBPOService.buscarPorId(cuentaId)
                .orElseThrow(() -> new BPOContenidoNoEncontradoException("Cuenta no encontrada"));

        List<UUID> cuentasAsignadas = cuentaBPOService.obtenerCuentaIdsPorUsuario(usuarioId);
        if (!cuentasAsignadas.contains(cuentaId)) {
            // Auto-asignar si el usuario no tiene permisos pero la cuenta existe
            cuentaBPOService.asignarUsuarioACuenta(usuarioId, cuentaId, usuarioId);
        }

        SubServicioBPO sub = cuentaBPOService.obtenerSubserviciosPorCuenta(cuentaId).stream()
                .filter(s -> s.getId().equals(subservicioId)).findFirst()
                .orElseThrow(() -> new BPOContenidoNoEncontradoException("Subcarpeta no encontrada"));

        String archivoUrl = null, archivoNombre = null, archivoMime = null;
        Long archivoTamano = null;
        if (dto.getArchivo() != null && !dto.getArchivo().isEmpty()) {
            var archivoInfo = fileUploadService.subirArchivo(dto.getArchivo(), "bpo");
            archivoUrl = archivoInfo.url();
            archivoNombre = archivoInfo.nombre();
            archivoMime = archivoInfo.mime();
            archivoTamano = archivoInfo.tamano();
        }

        String tipoValor = (dto.getTitulo() != null && !dto.getTitulo().isBlank()) ? "procedimiento" : "procedimiento";
        LocalDateTime vigenciaDesde = dto.getVigenciaDesde() != null ? dto.getVigenciaDesde() : LocalDateTime.now();
        LocalDateTime vigenciaHasta = dto.getVigenciaHasta() != null ? dto.getVigenciaHasta() : vigenciaDesde.plusYears(1);
        ContenidoBPO contenido = ContenidoBPO.builder()
                .tipo(tipoValor)
                .titulo(dto.getTitulo())
                .descripcionCorta(dto.getDescripcionCorta())
                .contenidoHtml(dto.getContenidoHtml())
                .metadatos(Map.of())
                .etiquetas(List.of())
                .estado("publicado")
                .vigenciaDesde(vigenciaDesde)
                .vigenciaHasta(vigenciaHasta)
                .cuenta(cuenta)
                .subservicio(sub)
                .archivoUrl(archivoUrl)
                .archivoNombre(archivoNombre)
                .archivoMime(archivoMime)
                .archivoTamano(archivoTamano)
                .creadoPor(usuarioId)
                .build();

        ContenidoBPO guardado = contenidoRepository.save(contenido);

        auditLogger.registrarEvento(
                "BPO_CONTENIDO_CREADO", usuarioId, "CREAR", "BPO_CONTENIDO",
                guardado.getId(),
                Map.of("titulo", dto.getTitulo(), "cuenta_id", cuentaId.toString())
        );

        return guardado;
    }

    public ContenidoBPO actualizarPublicacion(UUID id, ContenidoBPOCreateDTO dto, UUID usuarioId) {
        ContenidoBPO contenido = contenidoRepository.findById(id)
                .orElseThrow(() -> new BPOContenidoNoEncontradoException("Publicacion no encontrada"));
        contenido.setTitulo(dto.getTitulo());
        contenido.setDescripcionCorta(dto.getDescripcionCorta());
        contenido.setContenidoHtml(dto.getContenidoHtml());
        if (dto.getArchivo() != null && !dto.getArchivo().isEmpty()) {
            var archivoInfo = fileUploadService.subirArchivo(dto.getArchivo(), "bpo");
            contenido.setArchivoUrl(archivoInfo.url());
            contenido.setArchivoNombre(archivoInfo.nombre());
            contenido.setArchivoMime(archivoInfo.mime());
            contenido.setArchivoTamano(archivoInfo.tamano());
        }
        if (dto.getVigenciaDesde() != null) contenido.setVigenciaDesde(dto.getVigenciaDesde());
        if (dto.getVigenciaHasta() != null) contenido.setVigenciaHasta(dto.getVigenciaHasta());
        contenido.setActualizadoPor(usuarioId);
        contenido.setUpdatedAt(LocalDateTime.now());
        ContenidoBPO guardado = contenidoRepository.save(contenido);
        auditLogger.registrarEvento("BPO_CONTENIDO_ACTUALIZADO", usuarioId, "ACTUALIZAR",
                "BPO_CONTENIDO", id, Map.of("titulo", dto.getTitulo()));
        return guardado;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> obtenerResumen(UUID usuarioId) {
        List<UUID> cuentaIds = cuentaBPOService.obtenerCuentaIdsPorUsuario(usuarioId);
        long procedimientos = 0, noticias = 0, ofertas = 0;
        for (UUID cid : cuentaIds) {
            procedimientos += contenidoRepository.findByCuentaIdAndEstadoAndDeletedAtIsNullOrderByCreatedAtDesc(cid, "publicado")
                    .stream().filter(c -> "procedimiento".equals(c.getTipo())).count();
            noticias += contenidoRepository.findByCuentaIdAndEstadoAndDeletedAtIsNullOrderByCreatedAtDesc(cid, "publicado")
                    .stream().filter(c -> "noticia".equals(c.getTipo())).count();
            ofertas += contenidoRepository.findByCuentaIdAndEstadoAndDeletedAtIsNullOrderByCreatedAtDesc(cid, "publicado")
                    .stream().filter(c -> "oferta".equals(c.getTipo())).count();
        }
        return Map.of("procedimientosVigentes", procedimientos,
                      "noticiasRecientes", noticias,
                      "ofertasActivas", ofertas);
    }

    @Transactional(readOnly = true)
    public List<ContenidoBPO> buscarConFiltros(UUID usuarioId, String q, String tipo, String estado) {
        List<UUID> cuentaIds = cuentaBPOService.obtenerCuentaIdsPorUsuario(usuarioId);
        if (cuentaIds.isEmpty()) return List.of();

        String estadoBusqueda = (estado != null && !estado.isBlank()) ? estado : "publicado";
        List<ContenidoBPO> resultados = new ArrayList<>();

        for (UUID cid : cuentaIds) {
            List<ContenidoBPO> contents = contenidoRepository
                    .findByCuentaIdAndEstadoAndDeletedAtIsNullOrderByCreatedAtDesc(cid, estadoBusqueda);
            for (ContenidoBPO c : contents) {
                if (tipo != null && !tipo.isBlank() && !tipo.equals(c.getTipo())) continue;
                if (q != null && !q.isBlank()) {
                    String lowerQ = q.toLowerCase();
                    boolean match = (c.getTitulo() != null && c.getTitulo().toLowerCase().contains(lowerQ))
                            || (c.getDescripcionCorta() != null && c.getDescripcionCorta().toLowerCase().contains(lowerQ))
                            || (c.getContenidoHtml() != null && c.getContenidoHtml().toLowerCase().contains(lowerQ));
                    if (!match) continue;
                }
                resultados.add(c);
            }
        }
        return resultados;
    }

    public ContenidoBPO crearContenido(ContenidoBPOCreateDTO dto, UUID usuarioId, UUID cuentaId) {
        List<UUID> cuentasAsignadas = cuentaBPOService.obtenerCuentaIdsPorUsuario(usuarioId);
        if (!cuentasAsignadas.contains(cuentaId)) {
            throw new BPOCuentaNoAsignadaException("No tienes permisos para esta cuenta");
        }

        UUID subservicioId = dto.getSubservicioId();
        if (subservicioId == null) {
            List<SubServicioBPO> subs = cuentaBPOService.obtenerSubserviciosPorCuenta(cuentaId);
            if (subs.isEmpty()) {
                throw new BPOContenidoNoEncontradoException("La cuenta no tiene subcarpetas");
            }
            subservicioId = subs.get(0).getId();
        }

        return crearPublicacion(dto, usuarioId, cuentaId, subservicioId);
    }

    @Transactional
    public void cambiarEstado(UUID id, String estado, UUID usuarioId) {
        ContenidoBPO contenido = contenidoRepository.findById(id)
                .orElseThrow(() -> new BPOContenidoNoEncontradoException("Contenido no encontrado"));
        if ("publicado".equals(estado) && (contenido.getVigenciaDesde() == null || contenido.getVigenciaHasta() == null)) {
            throw new BPOEstadoInvalidoException("Debe establecer vigencia_desde y vigencia_hasta antes de publicar");
        }
        contenido.setEstado(estado);
        contenido.setActualizadoPor(usuarioId);
        contenido.setUpdatedAt(LocalDateTime.now());
        contenidoRepository.save(contenido);

        auditLogger.registrarEvento(
                "BPO_CONTENIDO_ESTADO_CAMBIADO", usuarioId, "ACTUALIZAR", "BPO_CONTENIDO",
                id, Map.of("estado", estado, "titulo", contenido.getTitulo())
        );
    }

    public void eliminarPublicacion(UUID id, UUID usuarioId, String motivo) {
        ContenidoBPO contenido = contenidoRepository.findById(id)
                .orElseThrow(() -> new BPOContenidoNoEncontradoException("Publicacin no encontrada"));
        contenido.setDeletedAt(LocalDateTime.now());
        contenido.setDeletedBy(usuarioId);
        contenido.setReason(motivo);
        contenidoRepository.save(contenido);

        auditLogger.registrarEvento(
                "BPO_CONTENIDO_ELIMINADO", usuarioId, "ELIMINAR", "BPO_CONTENIDO",
                id, Map.of("titulo", contenido.getTitulo(), "motivo", motivo != null ? motivo : "")
        );
    }
}
