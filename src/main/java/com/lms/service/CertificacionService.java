package com.lms.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lms.dto.*;
import com.lms.model.*;
import com.lms.repository.*;
import com.lms.util.AuditLogger;
import com.lms.util.HashUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class CertificacionService {

    private final CertificacionRepository certificacionRepo;
    private final PreguntaCertificacionRepository preguntaRepo;
    private final IntentoCertificacionRepository intentoRepo;
    private final CertificacionUsuarioRepository certUsuarioRepo;
    private final CodigoGeneratorService codigoGeneratorService;
    private final UsuarioRepository usuarioRepository;
    private final AuditLogger auditLogger;
    private final InscripcionService inscripcionService;
    private final NotificacionService notificacionService;

    public CertificacionService(CertificacionRepository certificacionRepo,
                                 PreguntaCertificacionRepository preguntaRepo,
                                 IntentoCertificacionRepository intentoRepo,
                                 CertificacionUsuarioRepository certUsuarioRepo,
                                 CodigoGeneratorService codigoGeneratorService,
                                 UsuarioRepository usuarioRepository,
                                 AuditLogger auditLogger,
                                 InscripcionService inscripcionService,
                                 NotificacionService notificacionService) {
        this.certificacionRepo = certificacionRepo;
        this.preguntaRepo = preguntaRepo;
        this.intentoRepo = intentoRepo;
        this.certUsuarioRepo = certUsuarioRepo;
        this.codigoGeneratorService = codigoGeneratorService;
        this.usuarioRepository = usuarioRepository;
        this.auditLogger = auditLogger;
        this.inscripcionService = inscripcionService;
        this.notificacionService = notificacionService;
    }

    public CertificacionResponse crear(CertificacionRequest request, UUID autorId) {
        String configJson;
        try {
            configJson = request.configuracion() != null ? objectMapper.writeValueAsString(request.configuracion()) : "{}";
        } catch (Exception e) {
            configJson = "{}";
        }
        Certificacion cert = Certificacion.builder()
                .titulo(request.titulo())
                .descripcion(request.descripcion())
                .pesoTotal(1.0)
                .notaMinima(request.notaMinima() != null ? request.notaMinima() : 4.0)
                .tiempoEstimadoMinutos(request.tiempoEstimadoMinutos())
                .configuracion(configJson)
                .codigo(codigoGeneratorService.generarCodigo("CER"))
                .activo(true)
                .build();
        cert.setHashSha256(HashUtils.sha256(cert.toString()));
        cert = certificacionRepo.save(cert);

        List<PreguntaCertificacion> preguntas = new ArrayList<>();
        for (PreguntaCertificacionRequest p : request.preguntas()) {
            PreguntaCertificacion pq = PreguntaCertificacion.builder()
                    .certificacion(cert)
                    .tipo(p.tipo())
                    .contenido(p.contenido())
                    .peso(p.peso() != null ? p.peso() : 1.0)
                    .opciones(p.opciones())
                    .respuestaCorrecta(p.respuestaCorrecta())
                    .multimediaUrl(p.multimediaUrl())
                    .orden(p.orden())
                    .build();
            pq.setHashSha256(HashUtils.sha256(pq.toString()));
            preguntas.add(pq);
        }
        preguntaRepo.saveAll(preguntas);
        cert.setPreguntas(preguntas);

        auditLogger.registrarEvento("CERTIFICACION", autorId, "CREAR", "certificacion", cert.getId(), Map.of("codigo", cert.getCodigo()));
        return toResponse(cert);
    }

    public CertificacionResponse actualizar(UUID id, CertificacionRequest request, UUID autorId) {
        Certificacion cert = certificacionRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Certificación no encontrada"));
        cert.setTitulo(request.titulo());
        cert.setDescripcion(request.descripcion());
        if (request.notaMinima() != null) cert.setNotaMinima(request.notaMinima());
        cert.setTiempoEstimadoMinutos(request.tiempoEstimadoMinutos());
        try {
            cert.setConfiguracion(request.configuracion() != null ? objectMapper.writeValueAsString(request.configuracion()) : "{}");
        } catch (Exception e) {
            cert.setConfiguracion("{}");
        }
        cert.setUpdatedAt(LocalDateTime.now());
        cert.setHashSha256(HashUtils.sha256(cert.toString()));

        cert.getPreguntas().clear();
        for (PreguntaCertificacionRequest p : request.preguntas()) {
            PreguntaCertificacion pq = PreguntaCertificacion.builder()
                    .certificacion(cert)
                    .tipo(p.tipo())
                    .contenido(p.contenido())
                    .peso(p.peso() != null ? p.peso() : 1.0)
                    .opciones(p.opciones())
                    .respuestaCorrecta(p.respuestaCorrecta())
                    .multimediaUrl(p.multimediaUrl())
                    .orden(p.orden())
                    .build();
            pq.setHashSha256(HashUtils.sha256(pq.toString()));
            cert.getPreguntas().add(pq);
        }
        cert = certificacionRepo.save(cert);

        auditLogger.registrarEvento("CERTIFICACION", autorId, "ACTUALIZAR", "certificacion", cert.getId(), Map.of("codigo", cert.getCodigo()));
        return toResponse(cert);
    }

    public void eliminar(UUID id, UUID autorId) {
        Certificacion cert = certificacionRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Certificación no encontrada"));
        cert.setDeletedAt(LocalDateTime.now());
        certificacionRepo.save(cert);
        auditLogger.registrarEvento("CERTIFICACION", autorId, "ELIMINAR", "certificacion", cert.getId(), Map.of("codigo", cert.getCodigo()));
    }

    @Transactional(readOnly = true)
    public CertificacionResponse buscarPorId(UUID id) {
        Certificacion cert = certificacionRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Certificación no encontrada"));
        return toResponse(cert);
    }

    @Transactional(readOnly = true)
    public CertificacionResponse buscarPorCodigo(String codigo) {
        Certificacion cert = certificacionRepo.findByCodigo(codigo)
                .orElseThrow(() -> new IllegalArgumentException("Certificación no encontrada"));
        return toResponse(cert);
    }

    @Transactional(readOnly = true)
    public List<CertificacionResponse> listarActivas() {
        return certificacionRepo.findByActivoTrueAndDeletedAtIsNull().stream()
                .map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<CertificacionResponse> listarTodas() {
        return certificacionRepo.findByDeletedAtIsNull().stream()
                .map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public Page<CertificacionResponse> listarPaginado(Pageable pageable) {
        return certificacionRepo.findAll(pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public List<PreguntaCertificacionResponse> obtenerPreguntas(UUID certificacionId) {
        return preguntaRepo.findByCertificacionIdOrderByOrdenAsc(certificacionId).stream()
                .map(this::toPreguntaResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<PreguntaCertificacionResponse> obtenerPreguntasSanitizadas(UUID certificacionId) {
        return preguntaRepo.findByCertificacionIdOrderByOrdenAsc(certificacionId).stream()
                .map(p -> PreguntaCertificacionResponse.builder()
                        .id(p.getId())
                        .certificacionId(p.getCertificacion().getId())
                        .tipo(p.getTipo())
                        .contenido(p.getContenido())
                        .peso(p.getPeso())
                        .opciones(p.getOpciones())
                        .multimediaUrl(p.getMultimediaUrl())
                        .orden(p.getOrden())
                        .build())
                .toList();
    }

    public void inscribirUsuario(UUID certificacionId, UUID usuarioId, UUID adminId) {
        if (certUsuarioRepo.findByCertificacionIdAndUsuarioId(certificacionId, usuarioId).isPresent()) {
            throw new IllegalArgumentException("El usuario ya está inscrito en esta certificación");
        }
        Certificacion cert = certificacionRepo.findById(certificacionId)
                .orElseThrow(() -> new IllegalArgumentException("Certificación no encontrada"));
        Usuario user = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        CertificacionUsuario cu = CertificacionUsuario.builder()
                .certificacion(cert)
                .usuario(user)
                .completado(false)
                .build();
        certUsuarioRepo.save(cu);
        try {
            notificacionService.enviarNotificacion(
                    usuarioId,
                    "sistema",
                    "Certificación asignada",
                    "Has sido inscrito en la certificación " + cert.getTitulo()
                            + " (" + cert.getCodigo() + ")",
                    "/certificaciones");
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(CertificacionService.class)
                    .error("Error al notificar inscripción del usuario {} en la certificación {}", usuarioId, certificacionId, e);
        }
        auditLogger.registrarEvento("CERTIFICACION", adminId, "INSCRIBIR", "certificacion", cert.getId(), Map.of("usuario", user.getEmail(), "codigo", cert.getCodigo()));
    }

    public InscripcionMasivaReport inscribirMasivo(UUID certificacionId, MultipartFile file) {
        Certificacion cert = certificacionRepo.findById(certificacionId)
                .orElseThrow(() -> new IllegalArgumentException("Certificación no encontrada"));
        List<String> exitosos = new ArrayList<>();
        List<String> noEncontrados = new ArrayList<>();
        List<String> yaInscritos = new ArrayList<>();
        List<String> errores = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String linea;
            while ((linea = br.readLine()) != null) {
                linea = linea.trim();
                if (linea.isEmpty() || linea.startsWith("#") || linea.startsWith("usuarioId") || linea.startsWith("email")) continue;
                try {
                    UUID uid = inscripcionService.resolverIdentificador(linea);
                    if (certUsuarioRepo.findByCertificacionIdAndUsuarioId(certificacionId, uid).isPresent()) {
                        yaInscritos.add(linea);
                    } else {
                        inscribirUsuario(certificacionId, uid, null);
                        exitosos.add(linea);
                    }
                } catch (IllegalArgumentException e) {
                    if (e.getMessage() != null && e.getMessage().contains("ya está inscrito")) {
                        yaInscritos.add(linea);
                    } else {
                        noEncontrados.add(linea);
                    }
                } catch (Exception e) {
                    errores.add(linea + ": " + e.getMessage());
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error al procesar archivo: " + e.getMessage());
        }

        return new InscripcionMasivaReport(
                exitosos.size() + noEncontrados.size() + yaInscritos.size() + errores.size(),
                exitosos.size(), noEncontrados.size(), yaInscritos.size(), errores.size(),
                List.of(), noEncontrados, yaInscritos, errores
        );
    }

    @Transactional(readOnly = true)
    public List<CertificacionUsuario> listarInscritos(UUID certificacionId) {
        return certUsuarioRepo.findByCertificacionId(certificacionId);
    }

    public void removerInscripcion(UUID certificacionId, UUID usuarioId) {
        CertificacionUsuario cu = certUsuarioRepo.findByCertificacionIdAndUsuarioId(certificacionId, usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Inscripción no encontrada"));
        certUsuarioRepo.delete(cu);
    }

    public IntentoCertificacionResponse iniciarIntento(UUID certificacionId, UUID usuarioId) {
        var existente = intentoRepo.findByCertificacionIdAndUsuarioIdAndEstado(certificacionId, usuarioId, "COMPLETADO");
        if (existente.isPresent()) {
            throw new IllegalStateException("Ya completaste esta certificación");
        }
        var enCurso = intentoRepo.findByCertificacionIdAndUsuarioIdAndEstado(certificacionId, usuarioId, "EN_CURSO");
        if (enCurso.isPresent()) {
            return toIntentoResponse(enCurso.get());
        }
        Certificacion cert = certificacionRepo.findById(certificacionId)
                .orElseThrow(() -> new IllegalArgumentException("Certificación no encontrada"));
        IntentoCertificacion intento = IntentoCertificacion.builder()
                .certificacion(cert)
                .usuario(usuarioRepository.findById(usuarioId).orElseThrow())
                .estado("EN_CURSO")
                .fechaInicio(LocalDateTime.now())
                .build();
        intento.setHashSha256(HashUtils.sha256(intento.toString()));
        intento = intentoRepo.save(intento);
        return toIntentoResponse(intento);
    }

    public IntentoCertificacionResponse entregarIntento(UUID intentoId, Map<String, Object> respuestasBody, UUID usuarioId) {
        IntentoCertificacion intento = intentoRepo.findById(intentoId)
                .orElseThrow(() -> new IllegalArgumentException("Intento no encontrado"));
        if (!intento.getUsuario().getId().equals(usuarioId)) {
            throw new SecurityException("Este intento no te pertenece");
        }
        if (!"EN_CURSO".equals(intento.getEstado())) {
            throw new IllegalStateException("Este intento ya fue entregado");
        }

        List<PreguntaCertificacion> preguntas = preguntaRepo.findByCertificacionIdOrderByOrdenAsc(intento.getCertificacion().getId());
        double notaTotal = 0;
        double pesoTotalAcumulado = 0;

        for (PreguntaCertificacion p : preguntas) {
            Object respuestaAlumno = respuestasBody.get(p.getId().toString());
            if (respuestaAlumno == null) continue;
            boolean correcta = verificarRespuesta(p, respuestaAlumno);
            if (correcta) {
                notaTotal += p.getPeso();
            }
            pesoTotalAcumulado += p.getPeso();
        }

        double notaFinal = pesoTotalAcumulado > 0 ? (notaTotal / pesoTotalAcumulado) * 100.0 : 0;
        boolean aprobado = notaFinal >= intento.getCertificacion().getNotaMinima();

        intento.setRespuestas(respuestasBody.toString());
        intento.setNota(notaFinal);
        intento.setAprobado(aprobado);
        intento.setEstado("COMPLETADO");
        intento.setFechaEntrega(LocalDateTime.now());
        intentoRepo.save(intento);

        var cu = certUsuarioRepo.findByCertificacionIdAndUsuarioId(intento.getCertificacion().getId(), usuarioId);
        cu.ifPresent(c -> {
            c.setNota(notaFinal);
            c.setAprobado(aprobado);
            c.setCompletado(true);
            certUsuarioRepo.save(c);
        });

        return toIntentoResponse(intento);
    }

    private final ObjectMapper objectMapper = new ObjectMapper();

    private boolean verificarRespuesta(PreguntaCertificacion p, Object respuestaAlumno) {
        String opcionesStr = p.getOpciones();
        if (opcionesStr != null && !opcionesStr.isBlank()) {
            try {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> opciones = objectMapper.readValue(
                        opcionesStr, new TypeReference<List<Map<String, Object>>>() {});
                if (respuestaAlumno instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<String> seleccionadas = (List<String>) respuestaAlumno;
                    List<String> correctas = opciones.stream()
                            .filter(o -> o.get("correcta") instanceof Boolean && (Boolean) o.get("correcta"))
                            .map(o -> o.get("texto") != null ? o.get("texto").toString() : "")
                            .toList();
                    return seleccionadas.size() == correctas.size()
                            && new java.util.HashSet<>(seleccionadas).containsAll(correctas);
                }
                for (Map<String, Object> op : opciones) {
                    Object val = op.get("valor") != null ? op.get("valor") : op.get("texto");
                    if (val != null && val.toString().equals(respuestaAlumno.toString())) {
                        Object correcta = op.get("correcta");
                        return correcta instanceof Boolean && (Boolean) correcta;
                    }
                }
            } catch (Exception e) {
                return false;
            }
            return false;
        }
        if (p.getRespuestaCorrecta() != null) {
            return p.getRespuestaCorrecta().trim().equalsIgnoreCase(respuestaAlumno.toString().trim());
        }
        return false;
    }

    @Transactional(readOnly = true)
    public IntentoCertificacionResponse buscarIntentoEnCurso(UUID certificacionId, UUID usuarioId) {
        return intentoRepo.findByCertificacionIdAndUsuarioIdAndEstado(certificacionId, usuarioId, "EN_CURSO")
                .map(this::toIntentoResponse)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public List<CertificacionResponse> listarMisCertificaciones(UUID usuarioId) {
        List<CertificacionUsuario> misCerts = certUsuarioRepo.findByUsuarioId(usuarioId);
        Set<UUID> ids = misCerts.stream().map(c -> c.getCertificacion().getId()).collect(Collectors.toSet());
        return certificacionRepo.findByActivoTrueAndDeletedAtIsNull().stream()
                .filter(c -> ids.contains(c.getId()))
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public long contarMisCertificaciones(UUID usuarioId) {
        return certUsuarioRepo.findByUsuarioId(usuarioId).size();
    }

    @Transactional(readOnly = true)
    public List<IntentoCertificacionResponse> resultados(UUID certificacionId, UUID usuarioId) {
        return intentoRepo.findByCertificacionIdAndUsuarioIdAndDeletedAtIsNull(certificacionId, usuarioId)
                .stream().map(this::toIntentoResponse).toList();
    }

    private CertificacionResponse toResponse(Certificacion cert) {
        List<PreguntaCertificacionResponse> preguntas = preguntaRepo.findByCertificacionIdOrderByOrdenAsc(cert.getId())
                .stream().map(this::toPreguntaResponse).toList();
        return CertificacionResponse.builder()
                .id(cert.getId())
                .titulo(cert.getTitulo())
                .descripcion(cert.getDescripcion())
                .pesoTotal(cert.getPesoTotal())
                .notaMinima(cert.getNotaMinima())
                .tiempoEstimadoMinutos(cert.getTiempoEstimadoMinutos())
                .configuracion(cert.getConfiguracion())
                .codigo(cert.getCodigo())
                .hashSha256(cert.getHashSha256())
                .activo(cert.getActivo())
                .createdAt(cert.getCreatedAt())
                .updatedAt(cert.getUpdatedAt())
                .preguntas(preguntas)
                .build();
    }

    private PreguntaCertificacionResponse toPreguntaResponse(PreguntaCertificacion p) {
        return PreguntaCertificacionResponse.builder()
                .id(p.getId())
                .certificacionId(p.getCertificacion().getId())
                .tipo(p.getTipo())
                .contenido(p.getContenido())
                .peso(p.getPeso())
                .opciones(p.getOpciones())
                .respuestaCorrecta(p.getRespuestaCorrecta())
                .multimediaUrl(p.getMultimediaUrl())
                .orden(p.getOrden())
                .createdAt(p.getCreatedAt())
                .build();
    }

    private IntentoCertificacionResponse toIntentoResponse(IntentoCertificacion i) {
        return IntentoCertificacionResponse.builder()
                .id(i.getId())
                .certificacionId(i.getCertificacion().getId())
                .usuarioId(i.getUsuario().getId())
                .respuestas(i.getRespuestas())
                .nota(i.getNota())
                .aprobado(i.getAprobado())
                .estado(i.getEstado())
                .fechaInicio(i.getFechaInicio())
                .fechaEntrega(i.getFechaEntrega())
                .createdAt(i.getCreatedAt())
                .build();
    }
}
