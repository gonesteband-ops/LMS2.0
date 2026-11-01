package com.lms.service;

import com.lms.model.Certificacion;
import com.lms.model.Curso;
import com.lms.model.Evaluacion;
import com.lms.model.EventoAuditoria;
import com.lms.model.Exportacion;
import com.lms.model.FirmaElectronica;
import com.lms.model.IncidenteSeguridad;
import com.lms.model.IntentoEvaluacion;
import com.lms.model.Inscripcion;
import com.lms.model.Notificacion;
import com.lms.model.PaqueteScorm;
import com.lms.model.Pregunta;
import com.lms.model.ScormDatoAlumno;
import com.lms.model.Usuario;
import com.lms.model.bpo.ContenidoBPO;
import com.lms.model.bpo.CuentaBPO;
import com.lms.model.bpo.SubServicioBPO;
import com.lms.repository.CertificacionRepository;
import com.lms.repository.CertificacionUsuarioRepository;
import com.lms.repository.CursoRepository;
import com.lms.repository.EvaluacionRepository;
import com.lms.repository.EventoAuditoriaRepository;
import com.lms.repository.ExportacionRepository;
import com.lms.repository.FirmaElectronicaRepository;
import com.lms.repository.IncidenteSeguridadRepository;
import com.lms.repository.IntentoCertificacionRepository;
import com.lms.repository.IntentoEvaluacionRepository;
import com.lms.repository.InscripcionRepository;
import com.lms.repository.NotificacionRepository;
import com.lms.repository.PaqueteScormRepository;
import com.lms.repository.PreguntaRepository;
import com.lms.repository.ScormDatoAlumnoRepository;
import com.lms.repository.UsuarioRepository;
import com.lms.repository.bpo.ContenidoBPORepository;
import com.lms.repository.bpo.CuentaBPORepository;
import com.lms.repository.bpo.SubServicioBPORepository;
import com.lms.util.AuditLogger;
import io.minio.GetObjectArgs;

import io.minio.GetObjectResponse;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.UUID;
import java.util.stream.Collectors;
/**
 * Servicio de exportación de reportes (XLSX via Apache POI).
 * <p>
 * Las exportaciones se procesan de forma asíncrona mediante Redis pub/sub.
 */
@Service
@Transactional
public class ExportacionService {

    private static final Logger log = LoggerFactory.getLogger(ExportacionService.class);
    private static final String REDIS_CHANNEL_EXPORTACIONES = "exportaciones";
    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();

    private final ExportacionRepository exportacionRepository;
    private final EvaluacionRepository evaluacionRepository;
    private final PaqueteScormRepository paqueteScormRepository;
    private final CursoRepository cursoRepository;
    private final InscripcionRepository inscripcionRepository;
    private final UsuarioRepository usuarioRepository;
    private final FirmaElectronicaRepository firmaElectronicaRepository;
    private final EventoAuditoriaRepository eventoAuditoriaRepository;
    private final ScormDatoAlumnoRepository scormDatoAlumnoRepository;
    private final PreguntaRepository preguntaRepository;
    private final IntentoEvaluacionRepository intentoRepository;
    private final MinioClient minioClient;
    private final AuditLogger auditLogger;
    private final CertificacionRepository certificacionRepository;
    private final IntentoCertificacionRepository intentoCertificacionRepository;
    private final CertificacionUsuarioRepository certificacionUsuarioRepository;
    private final CuentaBPORepository cuentaBPORepository;
    private final SubServicioBPORepository subServicioBPORepository;
    private final ContenidoBPORepository contenidoBPORepository;
    private final IncidenteSeguridadRepository incidenteSeguridadRepository;
    private final NotificacionRepository notificacionRepository;

    @Value("${app.minio.bucket-name}")
    private String bucketName;

    public ExportacionService(ExportacionRepository exportacionRepository,
                              EvaluacionRepository evaluacionRepository,
                              PaqueteScormRepository paqueteScormRepository,
                              CursoRepository cursoRepository,
                              InscripcionRepository inscripcionRepository,
                              UsuarioRepository usuarioRepository,
                              FirmaElectronicaRepository firmaElectronicaRepository,
                              EventoAuditoriaRepository eventoAuditoriaRepository,
                              ScormDatoAlumnoRepository scormDatoAlumnoRepository,
                              PreguntaRepository preguntaRepository,
                              IntentoEvaluacionRepository intentoRepository,
                              MinioClient minioClient,
                              AuditLogger auditLogger,
                              CertificacionRepository certificacionRepository,
                              IntentoCertificacionRepository intentoCertificacionRepository,
                              CertificacionUsuarioRepository certificacionUsuarioRepository,
                              CuentaBPORepository cuentaBPORepository,
                              SubServicioBPORepository subServicioBPORepository,
                              ContenidoBPORepository contenidoBPORepository,
                              IncidenteSeguridadRepository incidenteSeguridadRepository,
                              NotificacionRepository notificacionRepository) {
        this.exportacionRepository = exportacionRepository;
        this.evaluacionRepository = evaluacionRepository;
        this.paqueteScormRepository = paqueteScormRepository;
        this.cursoRepository = cursoRepository;
        this.inscripcionRepository = inscripcionRepository;
        this.usuarioRepository = usuarioRepository;
        this.firmaElectronicaRepository = firmaElectronicaRepository;
        this.eventoAuditoriaRepository = eventoAuditoriaRepository;
        this.scormDatoAlumnoRepository = scormDatoAlumnoRepository;
        this.preguntaRepository = preguntaRepository;
        this.intentoRepository = intentoRepository;
        this.minioClient = minioClient;
        this.auditLogger = auditLogger;
        this.certificacionRepository = certificacionRepository;
        this.intentoCertificacionRepository = intentoCertificacionRepository;
        this.certificacionUsuarioRepository = certificacionUsuarioRepository;
        this.cuentaBPORepository = cuentaBPORepository;
        this.subServicioBPORepository = subServicioBPORepository;
        this.contenidoBPORepository = contenidoBPORepository;
        this.incidenteSeguridadRepository = incidenteSeguridadRepository;
        this.notificacionRepository = notificacionRepository;
    }

    /**
     * Solicita una exportación y la procesa inmediatamente de forma síncrona.
     */
    public Exportacion solicitarExportacion(UUID solicitanteId, String tipoReporte, String filtros) {
        Exportacion exportacion = Exportacion.builder()
                .solicitanteId(solicitanteId)
                .tipoReporte(tipoReporte)
                .filtros(filtros)
                .estado("pendiente")
                .build();

        exportacion = exportacionRepository.save(exportacion);

        final UUID exportacionId = exportacion.getId();
        final String tipo = tipoReporte;

        log.debug("Exportacion guardada con ID: {} - procesando...", exportacionId);
        procesarExportacion(exportacionId);

        auditLogger.registrarEvento(
                "EXPORTACION_SOLICITADA",
                solicitanteId,
                "SOLICITAR",
                "EXPORTACION",
                exportacionId,
                Map.of("tipoReporte", tipo)
        );

        return exportacion;
    }

    /**
     * Obtiene el estado actual de una exportación.
     */
    @Transactional(readOnly = true)
    public Optional<Exportacion> obtenerEstado(UUID exportacionId) {
        return exportacionRepository.findById(exportacionId);
    }

    /**
     * Genera un archivo XLSX de ejemplo (implementación simplificada).
     * En Fase 2 se migrará a Celery para procesamiento asíncrono.
     */
    public byte[] generarExcelEjemplo(String[] headers, List<String[]> data) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Reporte");

            // Header style
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            // Create header row
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Data rows
            for (int rowIdx = 0; rowIdx < data.size(); rowIdx++) {
                Row row = sheet.createRow(rowIdx + 1);
                String[] rowData = data.get(rowIdx);
                for (int colIdx = 0; colIdx < rowData.length; colIdx++) {
                    row.createCell(colIdx).setCellValue(rowData[colIdx]);
                }
            }

            // Auto-size columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error generando archivo Excel", e);
        }
    }

    /**
     * Genera un reporte XLSX enriquecido de evaluaciones con 3 hojas:
     * 1) Resumen por evaluación, 2) Resultados por usuario, 3) Detalle de respuestas.
     *
     * @param exportacionId ID de la exportación para actualizar estado
     * @param codigo        Código específico (EVA001) para filtrar, o {@code null} para incluir todos
     */
    public void generarReporteEvaluaciones(UUID exportacionId, String codigo) {
        try {
            List<Evaluacion> evaluaciones;
            if (codigo != null && !codigo.isBlank()) {
                var opt = evaluacionRepository.findByCodigo(codigo.trim().toUpperCase());
                evaluaciones = opt.map(List::of).orElse(List.of());
                if (evaluaciones.isEmpty()) {
                    log.warn("No se encontr\u00f3 evaluaci\u00f3n con c\u00f3digo: {}", codigo);
                }
            } else {
                evaluaciones = evaluacionRepository.findAll();
            }

            try (Workbook workbook = new XSSFWorkbook()) {
                CellStyle headerStyle = crearHeaderStyle(workbook);

                // ── Hoja 1: Resumen por evaluación ──
                Sheet sheet1 = workbook.createSheet("Resumen por evaluación");
                String[] headers1 = {"ID Evaluación", "Código", "Título", "Curso ID",
                        "Curso Título", "Peso Total", "N° Preguntas", "Fecha Creación"};
                crearHeaderRow(sheet1, headers1, headerStyle);
                int rowIdx = 1;
                for (Evaluacion e : evaluaciones) {
                    Row row = sheet1.createRow(rowIdx++);
                    row.createCell(0).setCellValue(e.getId().toString());
                    row.createCell(1).setCellValue(e.getCodigo() != null ? e.getCodigo() : "—");
                    row.createCell(2).setCellValue(e.getTitulo());
                    row.createCell(3).setCellValue(e.getCurso() != null ? e.getCurso().getId().toString() : "—");
                    row.createCell(4).setCellValue(e.getCurso() != null ? e.getCurso().getTitulo() : "—");
                    row.createCell(5).setCellValue(String.valueOf(e.getPesoTotal()));
                    int numPreguntas = e.getPreguntas() != null ? e.getPreguntas().size()
                            : preguntaRepository.findByEvaluacionIdOrderByOrdenAsc(e.getId()).size();
                    row.createCell(6).setCellValue(String.valueOf(numPreguntas));
                    row.createCell(7).setCellValue(e.getCreatedAt() != null ? e.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : "—");
                }
                autoSizeColumns(sheet1, 8);

                // ── Hoja 2: Resultados por usuario (por curso de cada evaluación) ──
                Sheet sheet2 = workbook.createSheet("Resultados por usuario");
                String[] headers2 = {"ID Evaluación", "Título Evaluación", "ID Usuario",
                        "Email Usuario", "Nombre Usuario", "Estado", "Nota Final",
                        "Fecha Completado"};
                crearHeaderRow(sheet2, headers2, headerStyle);
                rowIdx = 1;
                for (Evaluacion e : evaluaciones) {
                    if (e.getCurso() == null) continue;
                    List<Inscripcion> inscripciones = inscripcionRepository.findByCursoIdAndDeletedAtIsNull(e.getCurso().getId());
                    for (Inscripcion ins : inscripciones) {
                        Row row = sheet2.createRow(rowIdx++);
                        row.createCell(0).setCellValue(e.getId().toString());
                        row.createCell(1).setCellValue(e.getTitulo());
                        row.createCell(2).setCellValue(ins.getUsuario().getId().toString());
                        row.createCell(3).setCellValue(ins.getUsuario().getEmail());
                        row.createCell(4).setCellValue(ins.getUsuario().getNombreCompleto());
                        row.createCell(5).setCellValue(ins.isCompletado() ? "Completado" : "Pendiente");
                        row.createCell(6).setCellValue(ins.getNota() != null ? String.valueOf(ins.getNota()) : "—");
                        row.createCell(7).setCellValue(ins.getFechaCompletado() != null
                                ? ins.getFechaCompletado().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : "—");
                    }
                }
                autoSizeColumns(sheet2, 8);

                // ── Hoja 3: Detalle de respuestas (por evaluación y pregunta) ──
                Sheet sheet3 = workbook.createSheet("Detalle respuestas");
                String[] headers3 = {"ID Evaluación", "ID Usuario", "Pregunta N°",
                        "Pregunta", "Tipo", "Respuesta Correcta", "Peso"};
                crearHeaderRow(sheet3, headers3, headerStyle);
                rowIdx = 1;
                for (Evaluacion e : evaluaciones) {
                    List<Pregunta> preguntas = e.getPreguntas() != null
                            ? e.getPreguntas()
                            : preguntaRepository.findByEvaluacionIdOrderByOrdenAsc(e.getId());
                    if (e.getCurso() == null) continue;
                    List<Inscripcion> inscripciones = inscripcionRepository.findByCursoIdAndDeletedAtIsNull(e.getCurso().getId());
                    for (Inscripcion ins : inscripciones) {
                        for (Pregunta p : preguntas) {
                            Row row = sheet3.createRow(rowIdx++);
                            row.createCell(0).setCellValue(e.getId().toString());
                            row.createCell(1).setCellValue(ins.getUsuario().getId().toString());
                            row.createCell(2).setCellValue(String.valueOf(p.getOrden()));
                            row.createCell(3).setCellValue(p.getContenido());
                            row.createCell(4).setCellValue(p.getTipo());
                            String correcta = p.getRespuestaCorrecta() != null ? p.getRespuestaCorrecta() : "—";
                            // Si la pregunta tiene opciones JSON, extraer texto de la opción correcta
                            if (p.getOpciones() != null && !p.getOpciones().isEmpty()) {
                                try {
                                    @SuppressWarnings("unchecked")
                                    List<Map<String, Object>> opcionesList = new com.fasterxml.jackson.databind.ObjectMapper()
                                            .readValue(p.getOpciones(), List.class);
                                    for (Map<String, Object> opt : opcionesList) {
                                        if (Boolean.TRUE.equals(opt.get("correcta"))) {
                                            correcta = opt.getOrDefault("texto", opt.getOrDefault("valor", correcta)).toString();
                                            break;
                                        }
                                    }
                                } catch (Exception ignored) {
                                    // fallback: usar respuestaCorrecta raw
                                }
                            }
                            row.createCell(5).setCellValue(correcta);
                            row.createCell(6).setCellValue(String.valueOf(p.getPeso()));
                        }
                    }
                }
                autoSizeColumns(sheet3, 7);

                // ── Escribir workbook a bytes ──
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                workbook.write(outputStream);
                byte[] excelBytes = outputStream.toByteArray();

                String objectName = "exportaciones/evaluaciones_" + exportacionId + ".xlsx";
                minioClient.putObject(
                        PutObjectArgs.builder()
                                .bucket(bucketName)
                                .object(objectName)
                                .stream(new ByteArrayInputStream(excelBytes), excelBytes.length, -1)
                                .contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                                .build()
                );

                String urlResultado = "/api/v1/exportaciones/" + exportacionId + "/descargar?object=" + objectName;
                actualizarEstado(exportacionId, "completado", urlResultado, null);
                log.info("Reporte enriquecido de evaluaciones generado: {} (3 hojas)", exportacionId);
            }
        } catch (Exception e) {
            log.error("Error generando reporte de evaluaciones", e);
            actualizarEstado(exportacionId, "error", null, e.getMessage());
        }
    }

    /**
     * Genera un reporte XLSX enriquecido de SCORM con 2 hojas:
     * 1) Paquetes SCORM, 2) Progreso por usuario.
     *
     * @param exportacionId ID de la exportación para actualizar estado
     */
    public void generarReporteScorm(UUID exportacionId, String codigo) {
        try {
            List<PaqueteScorm> paquetes;
            if (codigo != null && !codigo.isBlank()) {
                var opt = paqueteScormRepository.findByCodigoAndDeletedAtIsNull(codigo.trim().toUpperCase());
                paquetes = opt.map(List::of).orElse(List.of());
                if (paquetes.isEmpty()) {
                    log.warn("No se encontr\u00f3 paquete SCORM con c\u00f3digo: {}", codigo);
                }
            } else {
                paquetes = paqueteScormRepository.findAll();
            }

            try (Workbook workbook = new XSSFWorkbook()) {
                CellStyle headerStyle = crearHeaderStyle(workbook);

                // ── Hoja 1: Paquetes SCORM ──
                Sheet sheet1 = workbook.createSheet("Paquetes SCORM");
                String[] headers1 = {"ID Alfanumérico", "ID UUID", "Título", "Curso ID",
                        "Versión SCORM", "Tamaño (bytes)", "Hash SHA-256", "Fecha Creación"};
                crearHeaderRow(sheet1, headers1, headerStyle);
                int rowIdx = 1;
                for (PaqueteScorm p : paquetes) {
                    Row row = sheet1.createRow(rowIdx++);
                    row.createCell(0).setCellValue(p.getCodigo() != null ? p.getCodigo() : "—");
                    row.createCell(1).setCellValue(p.getId().toString());
                    row.createCell(2).setCellValue(p.getTitulo());
                    row.createCell(3).setCellValue(p.getCurso() != null ? p.getCurso().getId().toString() : "—");
                    row.createCell(4).setCellValue(p.getVersionScorm() != null ? p.getVersionScorm() : "—");
                    row.createCell(5).setCellValue(String.valueOf(p.getTamanoBytes()));
                    row.createCell(6).setCellValue(p.getHashSha256() != null ? p.getHashSha256() : "—");
                    row.createCell(7).setCellValue(p.getCreatedAt() != null ? p.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : "—");
                }
                autoSizeColumns(sheet1, 8);

                // ── Hoja 2: Progreso por usuario ──
                Sheet sheet2 = workbook.createSheet("Progreso por usuario");
                String[] headers2 = {"ID Paquete", "Título Paquete", "ID Usuario",
                        "Email Usuario", "% Avance", "Finalizó 100%", "Último Acceso"};
                crearHeaderRow(sheet2, headers2, headerStyle);
                rowIdx = 1;
                for (PaqueteScorm p : paquetes) {
                    List<UUID> usuarioIds = scormDatoAlumnoRepository.findDistinctUsuarioIdByPaqueteId(p.getId());
                    for (UUID uid : usuarioIds) {
                        List<ScormDatoAlumno> progreso = scormDatoAlumnoRepository
                                .findProgresoByPaqueteAndUsuario(p.getId(), uid);
                        String lessonStatus = "unknown";
                        String completionStatus = "unknown";
                        String sessionTime = "—";
                        for (ScormDatoAlumno d : progreso) {
                            switch (d.getClave()) {
                                case "cmi.core.lesson_status" -> lessonStatus = d.getValor();
                                case "cmi.completion_status" -> completionStatus = d.getValor();
                                case "cmi.core.session_time" -> sessionTime = d.getValor();
                            }
                        }
                        boolean completo = "completed".equalsIgnoreCase(lessonStatus)
                                || "passed".equalsIgnoreCase(lessonStatus)
                                || "completed".equalsIgnoreCase(completionStatus);

                        String email = usuarioRepository.findById(uid)
                                .map(Usuario::getEmail)
                                .orElse("—");

                        Row row = sheet2.createRow(rowIdx++);
                        row.createCell(0).setCellValue(p.getId().toString());
                        row.createCell(1).setCellValue(p.getTitulo());
                        row.createCell(2).setCellValue(uid.toString());
                        row.createCell(3).setCellValue(email);
                        // Estimar % avance basado en lesson_status
                        String pctAvance = switch (lessonStatus.toLowerCase()) {
                            case "not attempted" -> "0%";
                            case "incomplete" -> "25%";
                            case "browsed" -> "50%";
                            case "completed", "passed" -> "100%";
                            case "failed" -> "75%";
                            default -> completionStatus.equalsIgnoreCase("completed") ? "100%" : "—";
                        };
                        row.createCell(4).setCellValue(pctAvance);
                        row.createCell(5).setCellValue(completo ? "Sí" : "No");
                        row.createCell(6).setCellValue(sessionTime);
                    }
                }
                autoSizeColumns(sheet2, 7);

                // ── Escribir workbook a bytes ──
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                workbook.write(outputStream);
                byte[] excelBytes = outputStream.toByteArray();

                String objectName = "exportaciones/scorm_" + exportacionId + ".xlsx";
                minioClient.putObject(
                        PutObjectArgs.builder()
                                .bucket(bucketName)
                                .object(objectName)
                                .stream(new ByteArrayInputStream(excelBytes), excelBytes.length, -1)
                                .contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                                .build()
                );

                String urlResultado = "/api/v1/exportaciones/" + exportacionId + "/descargar?object=" + objectName;
                actualizarEstado(exportacionId, "completado", urlResultado, null);
                log.info("Reporte enriquecido SCORM generado: {} (2 hojas)", exportacionId);
            }
        } catch (Exception e) {
            log.error("Error generando reporte de SCORM", e);
            actualizarEstado(exportacionId, "error", null, e.getMessage());
        }
    }

    /**
     * Procesa una exportación según su tipo.
     *
     * @param exportacionId ID de la exportación
     */
    public void procesarExportacion(UUID exportacionId) {
        log.debug(">>> ExportacionService.procesarExportacion() buscando ID: {}", exportacionId);
        var opt = exportacionRepository.findById(exportacionId);
        if (opt.isEmpty()) {
            log.warn("⚠ ExportacionService.procesarExportacion() NO encontró registro con ID: {} (race condition: mensaje Redis llegó antes del commit)", exportacionId);
        }
        opt.ifPresent(exportacion -> {
            String tipo = exportacion.getTipoReporte();
            String filtros = exportacion.getFiltros();
            String codigo = extraerCodigoDeFiltro(filtros);
            log.info("Procesando exportación tipo '{}': {}", tipo, exportacionId);

            switch (tipo) {
                case "evaluaciones" -> generarReporteEvaluaciones(exportacionId, codigo);
                case "scorm" -> generarReporteScorm(exportacionId, codigo);
                case "cursos" -> generarReporteCursos(exportacionId);
                case "usuarios" -> generarReporteUsuarios(exportacionId, filtros);
                case "firmas" -> generarReporteFirmas(exportacionId);
                case "auditoria" -> generarReporteAuditoria(exportacionId);
                case "resultados" -> generarReporteResultados(exportacionId, codigo);
                case "certificaciones" -> generarReporteCertificaciones(exportacionId);
                case "resultados_certificaciones" -> generarReporteResultadosCertificaciones(exportacionId);
                case "gestores_contenido" -> generarReporteGestores(exportacionId);
                case "publicaciones_bpo" -> generarReportePublicacionesBpo(exportacionId);
                case "incidentes_seguridad" -> generarReporteIncidentes(exportacionId);
                case "notificaciones" -> generarReporteNotificaciones(exportacionId);
                default -> {
                    log.warn("Tipo de reporte no soportado: {}", tipo);
                    actualizarEstado(exportacionId, "error", null, "Tipo de reporte no soportado: " + tipo);
                }
            }
        });
    }

    /**
     * Actualiza el estado de una exportación.
     */
    public void actualizarEstado(UUID exportacionId, String estado, String urlResultado, String errorMensaje) {
        exportacionRepository.findById(exportacionId).ifPresent(exportacion -> {
            exportacion.setEstado(estado);
            if (urlResultado != null) {
                exportacion.setUrlResultado(urlResultado);
            }
            if (errorMensaje != null) {
                exportacion.setErrorMensaje(errorMensaje);
            }
            if ("completado".equals(estado) || "error".equals(estado)) {
                exportacion.setCompletedAt(LocalDateTime.now());
            }
            exportacionRepository.save(exportacion);
        });
    }

    /**
     * Genera un reporte XLSX con todos los cursos, sus inscritos y estado de acceso.
     *
     * @param exportacionId ID de la exportación para actualizar estado
     */
    public void generarReporteCursos(UUID exportacionId) {
        try {
            List<Curso> cursos = cursoRepository.findByDeletedAtIsNull(Pageable.unpaged()).getContent();

            String[] headers = {"ID Curso", "Título", "Estado", "Categoría",
                    "Versión", "Fecha Creación", "Inscritos", "Completados", "Último Acceso"};
            List<String[]> data = cursos.stream().map(c -> {
                long inscritos = inscripcionRepository.countByCursoIdAndDeletedAtIsNull(c.getId());
                long completados = inscripcionRepository.countByCursoIdAndCompletadoTrueAndDeletedAtIsNull(c.getId());
                String ultimoAcceso = inscripcionRepository.findUltimoAccesoByCursoId(c.getId())
                        .map(dt -> dt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                        .orElse("—");
                return new String[]{
                        c.getId().toString(),
                        c.getTitulo(),
                        c.getEstado(),
                        c.getCategoria() != null ? c.getCategoria() : "—",
                        String.valueOf(c.getVersionActual()),
                        c.getCreatedAt() != null ? c.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : "—",
                        String.valueOf(inscritos),
                        String.valueOf(completados),
                        ultimoAcceso
                };
            }).toList();

            byte[] excelBytes = generarExcelEjemplo(headers, data);
            String objectName = "exportaciones/cursos_" + exportacionId + ".xlsx";

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .stream(new ByteArrayInputStream(excelBytes), excelBytes.length, -1)
                            .contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                            .build()
            );

            String urlResultado = "/api/v1/exportaciones/" + exportacionId + "/descargar?object=" + objectName;
            actualizarEstado(exportacionId, "completado", urlResultado, null);

            log.info("Reporte de cursos generado: {} ({} cursos)", exportacionId, cursos.size());
        } catch (Exception e) {
            log.error("Error generando reporte de cursos", e);
            actualizarEstado(exportacionId, "error", null, e.getMessage());
        }
    }

    /**
     * Descarga un reporte XLSX desde MinIO.
     *
     * @param objectName nombre del objeto en MinIO (ej: "exportaciones/evaluaciones_xxx.xlsx")
     * @return bytes del archivo XLSX
     */
    public byte[] descargarReporte(String objectName) {
        try {
            GetObjectResponse response = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .build());
            return response.readAllBytes();
        } catch (Exception e) {
            log.error("Error descargando reporte desde MinIO: {}", objectName, e);
            throw new RuntimeException("Error descargando reporte: " + objectName, e);
        }
    }

    /**
     * Genera un reporte XLSX con usuarios según el filtro seleccionado:
     * - all/activo/rol → usuarios activos (findAllActive)
     * - estado=en-proceso → usuarios en período de gracia de baja
     * - estado=eliminados → usuarios anonimizados o admin soft-delete
     *
     * @param exportacionId ID de la exportación para actualizar estado
     * @param filtros       JSON con filtros (all, activo, rol, estado)
     */
    public void generarReporteUsuarios(UUID exportacionId, String filtros) {
        try {
            String estadoFilter = extraerEstadoDeFiltro(filtros);
            boolean incluirFechaBaja = "en-proceso".equals(estadoFilter) || "eliminados".equals(estadoFilter);

            List<Usuario> usuarios;
            if ("en-proceso".equals(estadoFilter)) {
                usuarios = usuarioRepository.findEnProcesoEliminacion(LocalDateTime.now(), Pageable.unpaged()).getContent();
            } else if ("eliminados".equals(estadoFilter)) {
                usuarios = usuarioRepository.findEliminados(LocalDateTime.now(), Pageable.unpaged()).getContent();
            } else {
                usuarios = usuarioRepository.findAllActive(Pageable.unpaged()).getContent();
                String rolFiltro = extraerRolDeFiltro(filtros);
                if (rolFiltro != null) {
                    usuarios = usuarios.stream()
                            .filter(u -> u.getRoles().stream().anyMatch(r -> r.getNombre().equals(rolFiltro)))
                            .toList();
                }
            }

            String[] headers;
            if (incluirFechaBaja) {
                headers = new String[]{"ID Usuario", "Email", "Nombre Completo", "RUT",
                        "Roles", "Estado", "Fecha Baja", "MFA Habilitado", "Consentimiento", "Último Login",
                        "Fecha Creación", "Hash SHA-256"};
            } else {
                headers = new String[]{"ID Usuario", "Email", "Nombre Completo", "RUT",
                        "Roles", "Estado", "MFA Habilitado", "Consentimiento", "Último Login",
                        "Fecha Creación", "Hash SHA-256"};
            }

            List<String[]> data = usuarios.stream().map(u -> {
                String roles = u.getRoles().stream()
                        .map(r -> r.getNombre())
                        .collect(Collectors.joining(", "));
                List<String> row = new ArrayList<>();
                row.add(u.getId().toString());
                row.add(u.getEmail());
                row.add(u.getNombreCompleto());
                row.add(u.getRut() != null ? u.getRut() : "—");
                row.add(roles);
                row.add(u.getEstado());
                if (incluirFechaBaja) {
                    row.add(u.getDeletedAt() != null ? u.getDeletedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : "—");
                }
                row.add(u.isMfaHabilitado() ? "Sí" : "No");
                row.add(u.isConsentimientoOtorgado() ? "Sí" : "No");
                row.add(u.getUltimoLogin() != null ? u.getUltimoLogin().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : "—");
                row.add(u.getCreatedAt() != null ? u.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : "—");
                row.add(u.getHashSha256());
                return row.toArray(new String[0]);
            }).toList();

            String suffix = switch (estadoFilter) {
                case "en-proceso" -> "en-proceso";
                case "eliminados" -> "eliminados";
                default -> "";
            };
            String objectName = "exportaciones/usuarios" + (suffix.isEmpty() ? "" : "-" + suffix) + "_" + exportacionId + ".xlsx";

            byte[] excelBytes = generarExcelEjemplo(headers, data);

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .stream(new ByteArrayInputStream(excelBytes), excelBytes.length, -1)
                            .contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                            .build()
            );

            String urlResultado = "/api/v1/exportaciones/" + exportacionId + "/descargar?object=" + objectName;
            actualizarEstado(exportacionId, "completado", urlResultado, null);

            String label = switch (estadoFilter) {
                case "en-proceso" -> "en proceso de eliminación";
                case "eliminados" -> "eliminados";
                default -> "de usuarios";
            };
            log.info("Reporte {} generado: {} ({} usuarios)", label, exportacionId, usuarios.size());
        } catch (Exception e) {
            log.error("Error generando reporte de usuarios", e);
            actualizarEstado(exportacionId, "error", null, e.getMessage());
        }
    }

    /**
     * Genera un reporte XLSX con todas las firmas electrónicas.
     *
     * @param exportacionId ID de la exportación para actualizar estado
     */
    public void generarReporteFirmas(UUID exportacionId) {
        try {
            List<FirmaElectronica> firmas = firmaElectronicaRepository.findAll();

            String[] headers = {"ID Firma", "Usuario Email", "Documento ID",
                    "IP Origen", "Timestamp", "Sistema Operativo",
                    "Geolocalización", "Hash Documento", "Hash SHA-256", "Hash Anterior"};
            List<String[]> data = firmas.stream().map(f -> new String[]{
                    f.getId().toString(),
                    f.getUsuario() != null ? f.getUsuario().getEmail() : "—",
                    f.getDocumento() != null ? f.getDocumento().getId().toString() : "—",
                    f.getIpOrigen(),
                    f.getTimestamp() != null ? f.getTimestamp().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : "—",
                    f.getSistemaOperativo() != null ? f.getSistemaOperativo() : "—",
                    f.getGeolocalizacion() != null ? f.getGeolocalizacion() : "—",
                    f.getHashDocumento(),
                    f.getHashSha256(),
                    f.getHashAnterior() != null ? f.getHashAnterior() : "—"
            }).toList();

            byte[] excelBytes = generarExcelEjemplo(headers, data);
            String objectName = "exportaciones/firmas_" + exportacionId + ".xlsx";

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .stream(new ByteArrayInputStream(excelBytes), excelBytes.length, -1)
                            .contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                            .build()
            );

            String urlResultado = "/api/v1/exportaciones/" + exportacionId + "/descargar?object=" + objectName;
            actualizarEstado(exportacionId, "completado", urlResultado, null);

            log.info("Reporte de firmas electrónicas generado: {} ({} firmas)", exportacionId, firmas.size());
        } catch (Exception e) {
            log.error("Error generando reporte de firmas electrónicas", e);
            actualizarEstado(exportacionId, "error", null, e.getMessage());
        }
    }

    /**
     * Genera un reporte XLSX con todos los eventos de auditoría.
     *
     * @param exportacionId ID de la exportación para actualizar estado
     */
    public void generarReporteAuditoria(UUID exportacionId) {
        try {
            List<EventoAuditoria> eventos = eventoAuditoriaRepository.findAll();

            String[] headers = {"ID Evento", "Tipo Evento", "Actor ID", "Acción",
                    "Recurso Tipo", "Recurso ID", "Metadata", "Hash SHA-256",
                    "Hash Anterior", "Fecha Creación"};
            List<String[]> data = eventos.stream().map(e -> new String[]{
                    e.getId().toString(),
                    e.getTipoEvento(),
                    e.getActorId() != null ? e.getActorId().toString() : "—",
                    e.getAccion(),
                    e.getRecursoTipo() != null ? e.getRecursoTipo() : "—",
                    e.getRecursoId() != null ? e.getRecursoId().toString() : "—",
                    e.getMetadata() != null ? e.getMetadata().toString() : "—",
                    e.getHashSha256(),
                    e.getHashAnterior() != null ? e.getHashAnterior() : "—",
                    e.getCreatedAt() != null ? e.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : "—"
            }).toList();

            byte[] excelBytes = generarExcelEjemplo(headers, data);
            String objectName = "exportaciones/auditoria_" + exportacionId + ".xlsx";

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .stream(new ByteArrayInputStream(excelBytes), excelBytes.length, -1)
                            .contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                            .build()
            );

            String urlResultado = "/api/v1/exportaciones/" + exportacionId + "/descargar?object=" + objectName;
            actualizarEstado(exportacionId, "completado", urlResultado, null);

            log.info("Reporte de auditoría generado: {} ({} eventos)", exportacionId, eventos.size());
        } catch (Exception e) {
            log.error("Error generando reporte de auditoría", e);
            actualizarEstado(exportacionId, "error", null, e.getMessage());
        }
    }

    /**
     * Lista exportaciones activas (no eliminadas) de un usuario.
     */
    @Transactional(readOnly = true)
    public List<Exportacion> listarPorSolicitante(UUID solicitanteId) {
        return exportacionRepository.findBySolicitanteIdAndDeletedAtIsNullOrderByCreatedAtDesc(solicitanteId);
    }

    /**
     * Limpia el historial de exportaciones del solicitante (soft-delete).
     *
     * @param solicitanteId ID del usuario
     * @return cantidad de registros marcados como eliminados
     */
    @Transactional
    public int limpiarHistorial(UUID solicitanteId) {
        int count = exportacionRepository.softDeleteBySolicitanteId(solicitanteId, LocalDateTime.now());
        if (count > 0) {
            log.info("Historial limpiado para usuario {}: {} exportaciones marcadas como eliminadas", solicitanteId, count);
            auditLogger.registrarEvento(
                    "EXPORTACIONES_HISTORIAL_LIMPIADO",
                    solicitanteId,
                    "DELETE",
                    "EXPORTACION",
                    null,
                    Map.of("cantidad", String.valueOf(count))
            );
        }
        return count;
    }

    // ──────────────────────────────────────────────
    // Helper methods for enriched multi-sheet reports
    // ──────────────────────────────────────────────

    /**
     * Creates a reusable header CellStyle with dark blue background and bold white font.
     */
    /**
     * Extrae el campo 'codigo' del JSON de filtros, o retorna null si no existe.
     */
    private String extraerCodigoDeFiltro(String filtros) {
        if (filtros == null || filtros.isBlank()) return null;
        try {
            var map = JSON_MAPPER.readValue(filtros, java.util.Map.class);
            Object c = map.get("codigo");
            return c != null ? c.toString().trim() : null;
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private String extraerRolDeFiltro(String filtros) {
        if (filtros == null || filtros.isBlank()) return null;
        try {
            var map = JSON_MAPPER.readValue(filtros, java.util.Map.class);
            Object r = map.get("rol");
            return r != null ? r.toString().trim() : null;
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private String extraerEstadoDeFiltro(String filtros) {
        if (filtros == null || filtros.isBlank()) return null;
        try {
            var map = JSON_MAPPER.readValue(filtros, java.util.Map.class);
            Object e = map.get("estado");
            return e != null ? e.toString().trim() : null;
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private CellStyle crearHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    /**
     * Creates a header row on the given sheet using the provided headers and style.
     */
    private void crearHeaderRow(Sheet sheet, String[] headers, CellStyle style) {
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(style);
        }
    }

    /**
     * Auto-sizes all columns in the sheet up to the given count.
     */
    private void autoSizeColumns(Sheet sheet, int numColumns) {
        for (int i = 0; i < numColumns; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    public void generarReporteResultados(UUID exportacionId, String codigo) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Resultados");
            Evaluacion ev = evaluacionRepository.findByCodigo(codigo).orElse(null);
            if (ev == null) {
                actualizarEstado(exportacionId, "error", null, "Evaluacion no encontrada: " + codigo);
                return;
            }
            List<IntentoEvaluacion> intentos = intentoRepository.findByEvaluacionIdAndEstadoAndDeletedAtIsNull(ev.getId(), "COMPLETADO");
            List<Pregunta> preguntas = ev.getPreguntas();
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("Alumno");
            header.createCell(1).setCellValue("Email");
            header.createCell(2).setCellValue("Nota");
            header.createCell(3).setCellValue("Aprobado");
            int col = 4;
            for (int i = 0; i < preguntas.size(); i++) {
                Pregunta p = preguntas.get(i);
                header.createCell(col++).setCellValue("P" + (i + 1) + " (" + p.getPeso() + "%)");
            }
            int rowNum = 1;
            for (IntentoEvaluacion ie : intentos) {
                Usuario u = usuarioRepository.findById(ie.getUsuario().getId()).orElse(null);
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(u != null ? u.getNombreCompleto() : "N/A");
                row.createCell(1).setCellValue(u != null ? u.getEmail() : "N/A");
                row.createCell(2).setCellValue(ie.getNota() != null ? ie.getNota() : 0.0);
                row.createCell(3).setCellValue(Boolean.TRUE.equals(ie.getAprobado()) ? "SI" : "NO");
                Map<String, Object> resp = ie.getRespuestas();
                if (resp != null && resp.get("detalle") instanceof List) {
                    List<Map<String, Object>> detalles = (List<Map<String, Object>>) resp.get("detalle");
                    col = 4;
                    for (int i = 0; i < preguntas.size(); i++) {
                        String result = "-";
                        if (i < detalles.size()) {
                            result = Boolean.TRUE.equals(detalles.get(i).get("correcta")) ? "CORRECTA" : "INCORRECTA";
                        }
                        row.createCell(col++).setCellValue(result);
                    }
                }
            }
            for (int i = 0; i <= col; i++) {
                sheet.autoSizeColumn(i);
            }
            String objectName = "exportaciones/resultados_" + codigo + ".xlsx";
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            workbook.write(baos);
            byte[] bytes = baos.toByteArray();
            minioClient.putObject(PutObjectArgs.builder().bucket(bucketName).object(objectName)
                    .stream(new java.io.ByteArrayInputStream(bytes), bytes.length, -1)
                    .contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet").build());
            String urlResultado = "/api/v1/exportaciones/" + exportacionId + "/descargar?object=" + objectName;
            actualizarEstado(exportacionId, "completado", urlResultado, null);
        } catch (Exception e) {
            log.error("Error generando reporte de resultados", e);
            actualizarEstado(exportacionId, "error", null, e.getMessage());
        }
    }

    public void generarReporteCertificaciones(UUID exportacionId) {
        try {
            List<Certificacion> list = certificacionRepository.findByDeletedAtIsNull();
            String[] headers = {"Código", "Título", "Descripción", "Nota Mínima (%)", "Tiempo (min)", "Activo", "Creado"};
            List<String[]> data = list.stream().map(c -> new String[]{
                c.getCodigo(), c.getTitulo(),
                c.getDescripcion() != null ? c.getDescripcion() : "—",
                String.valueOf(c.getNotaMinima()),
                c.getTiempoEstimadoMinutos() != null ? String.valueOf(c.getTiempoEstimadoMinutos()) : "—",
                c.getActivo() ? "Sí" : "No",
                c.getCreatedAt() != null ? c.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : "—"
            }).toList();
            byte[] excelBytes = generarExcelEjemplo(headers, data);
            String objectName = "exportaciones/certificaciones_" + exportacionId + ".xlsx";
            minioClient.putObject(PutObjectArgs.builder().bucket(bucketName).object(objectName)
                .stream(new ByteArrayInputStream(excelBytes), excelBytes.length, -1)
                .contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet").build());
            String urlResultado = "/api/v1/exportaciones/" + exportacionId + "/descargar?object=" + objectName;
            actualizarEstado(exportacionId, "completado", urlResultado, null);
        } catch (Exception e) {
            log.error("Error generando reporte de certificaciones", e);
            actualizarEstado(exportacionId, "error", null, e.getMessage());
        }
    }

    public void generarReporteResultadosCertificaciones(UUID exportacionId) {
        try {
            var intentos = intentoCertificacionRepository.findAll();
            String[] headers = {"Certificación", "Código", "Usuario", "Email", "Nota (%)", "Aprobado", "Fecha Inicio", "Fecha Entrega", "Estado"};
            List<String[]> data = intentos.stream().map(i -> new String[]{
                i.getCertificacion().getTitulo(),
                i.getCertificacion().getCodigo(),
                i.getUsuario().getNombreCompleto(),
                i.getUsuario().getEmail(),
                i.getNota() != null ? String.format("%.1f", i.getNota()) : "—",
                i.getAprobado() != null ? (i.getAprobado() ? "Aprobado" : "Reprobado") : "—",
                i.getFechaInicio() != null ? i.getFechaInicio().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : "—",
                i.getFechaEntrega() != null ? i.getFechaEntrega().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : "—",
                i.getEstado()
            }).toList();
            byte[] excelBytes = generarExcelEjemplo(headers, data);
            String objectName = "exportaciones/resultados_certificaciones_" + exportacionId + ".xlsx";
            minioClient.putObject(PutObjectArgs.builder().bucket(bucketName).object(objectName)
                .stream(new ByteArrayInputStream(excelBytes), excelBytes.length, -1)
                .contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet").build());
            String urlResultado = "/api/v1/exportaciones/" + exportacionId + "/descargar?object=" + objectName;
            actualizarEstado(exportacionId, "completado", urlResultado, null);
        } catch (Exception e) {
            log.error("Error generando reporte de resultados certificaciones", e);
            actualizarEstado(exportacionId, "error", null, e.getMessage());
        }
    }

    public void generarReporteGestores(UUID exportacionId) {
        try {
            List<CuentaBPO> list = cuentaBPORepository.findAll().stream()
                .filter(c -> c.getDeletedAt() == null).toList();
            String[] headers = {"Código", "Nombre", "Activo", "Subcarpetas", "Total Publicaciones", "Creado"};
            List<String[]> data = list.stream().map(c -> {
                long subs = subServicioBPORepository.findByCuentaCodigo(c.getCodigo()).size();
                long pubs = contenidoBPORepository.findByCuentaIdAndEstadoAndDeletedAtIsNullOrderByCreatedAtDesc(c.getId(), "publicado").size();
                return new String[]{
                    c.getCodigo(), c.getNombre(),
                    c.isActiva() ? "Sí" : "No",
                    String.valueOf(subs), String.valueOf(pubs),
                    c.getCreatedAt() != null ? c.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : "—"
                };
            }).toList();
            byte[] excelBytes = generarExcelEjemplo(headers, data);
            String objectName = "exportaciones/gestores_contenido_" + exportacionId + ".xlsx";
            minioClient.putObject(PutObjectArgs.builder().bucket(bucketName).object(objectName)
                .stream(new ByteArrayInputStream(excelBytes), excelBytes.length, -1)
                .contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet").build());
            String urlResultado = "/api/v1/exportaciones/" + exportacionId + "/descargar?object=" + objectName;
            actualizarEstado(exportacionId, "completado", urlResultado, null);
        } catch (Exception e) {
            log.error("Error generando reporte de gestores", e);
            actualizarEstado(exportacionId, "error", null, e.getMessage());
        }
    }

    public void generarReportePublicacionesBpo(UUID exportacionId) {
        try {
            List<ContenidoBPO> list = contenidoBPORepository.findAll().stream()
                .filter(c -> c.getDeletedAt() == null).toList();
            String[] headers = {"ID", "Título", "Tipo", "Descripción", "Estado", "Gestor", "Subcarpeta", "Archivo", "Creado"};
            List<String[]> data = list.stream().map(c -> new String[]{
                c.getId().toString(), c.getTitulo(), c.getTipo(),
                c.getDescripcionCorta() != null ? c.getDescripcionCorta() : "—",
                c.getEstado(),
                c.getCuenta() != null ? c.getCuenta().getNombre() : "—",
                c.getSubservicio() != null ? c.getSubservicio().getNombre() : "—",
                c.getArchivoNombre() != null ? c.getArchivoNombre() : "—",
                c.getCreatedAt() != null ? c.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : "—"
            }).toList();
            byte[] excelBytes = generarExcelEjemplo(headers, data);
            String objectName = "exportaciones/publicaciones_bpo_" + exportacionId + ".xlsx";
            minioClient.putObject(PutObjectArgs.builder().bucket(bucketName).object(objectName)
                .stream(new ByteArrayInputStream(excelBytes), excelBytes.length, -1)
                .contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet").build());
            String urlResultado = "/api/v1/exportaciones/" + exportacionId + "/descargar?object=" + objectName;
            actualizarEstado(exportacionId, "completado", urlResultado, null);
        } catch (Exception e) {
            log.error("Error generando reporte de publicaciones BPO", e);
            actualizarEstado(exportacionId, "error", null, e.getMessage());
        }
    }

    public void generarReporteIncidentes(UUID exportacionId) {
        try {
            List<IncidenteSeguridad> list = incidenteSeguridadRepository.findAll();
            String[] headers = {"ID", "Tipo", "Nivel Riesgo", "Descripción", "IP Origen", "Estado", "Fecha Detección"};
            List<String[]> data = list.stream().map(i -> new String[]{
                i.getId().toString(),
                i.getTipo() != null ? i.getTipo() : "—",
                i.getNivelRiesgo() != null ? i.getNivelRiesgo() : "—",
                i.getDescripcion() != null ? i.getDescripcion() : "—",
                i.getIpOrigen() != null ? i.getIpOrigen() : "—",
                i.getEstado() != null ? i.getEstado() : "—",
                i.getFechaDeteccion() != null ? i.getFechaDeteccion().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : "—"
            }).toList();
            byte[] excelBytes = generarExcelEjemplo(headers, data);
            String objectName = "exportaciones/incidentes_" + exportacionId + ".xlsx";
            minioClient.putObject(PutObjectArgs.builder().bucket(bucketName).object(objectName)
                .stream(new ByteArrayInputStream(excelBytes), excelBytes.length, -1)
                .contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet").build());
            String urlResultado = "/api/v1/exportaciones/" + exportacionId + "/descargar?object=" + objectName;
            actualizarEstado(exportacionId, "completado", urlResultado, null);
        } catch (Exception e) {
            log.error("Error generando reporte de incidentes de seguridad", e);
            actualizarEstado(exportacionId, "error", null, e.getMessage());
        }
    }

    public void generarReporteNotificaciones(UUID exportacionId) {
        try {
            List<Notificacion> list = notificacionRepository.findAll();
            String[] headers = {"ID", "Usuario Email", "Título", "Tipo", "Leída", "Fecha"};
            List<String[]> data = list.stream().map(n -> new String[]{
                n.getId().toString(),
                n.getUsuario() != null ? n.getUsuario().getEmail() : "—",
                n.getTitulo() != null ? n.getTitulo() : "—",
                n.getTipo() != null ? n.getTipo() : "—",
                n.isLeida() ? "Sí" : "No",
                n.getCreatedAt() != null ? n.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : "—"
            }).toList();
            byte[] excelBytes = generarExcelEjemplo(headers, data);
            String objectName = "exportaciones/notificaciones_" + exportacionId + ".xlsx";
            minioClient.putObject(PutObjectArgs.builder().bucket(bucketName).object(objectName)
                .stream(new ByteArrayInputStream(excelBytes), excelBytes.length, -1)
                .contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet").build());
            String urlResultado = "/api/v1/exportaciones/" + exportacionId + "/descargar?object=" + objectName;
            actualizarEstado(exportacionId, "completado", urlResultado, null);
        } catch (Exception e) {
            log.error("Error generando reporte de notificaciones", e);
            actualizarEstado(exportacionId, "error", null, e.getMessage());
        }
    }
}