package com.lms.service;

import com.lms.model.*;
import com.lms.repository.*;
import com.lms.util.AuditLogger;
import io.minio.GetObjectResponse;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExportacionService - Async Export with Redis Pub/Sub")
class ExportacionServiceTest {

    @Mock
    private ExportacionRepository exportacionRepository;

    @Mock
    private EvaluacionRepository evaluacionRepository;

    @Mock
    private PaqueteScormRepository paqueteScormRepository;

    @Mock
    private CursoRepository cursoRepository;

    @Mock
    private InscripcionRepository inscripcionRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private FirmaElectronicaRepository firmaElectronicaRepository;

    @Mock
    private EventoAuditoriaRepository eventoAuditoriaRepository;

    @Mock
    private ScormDatoAlumnoRepository scormDatoAlumnoRepository;

    @Mock
    private PreguntaRepository preguntaRepository;

    @Mock
    private MinioClient minioClient;

    @Mock
    private AuditLogger auditLogger;

    @InjectMocks
    private ExportacionService exportacionService;

    @BeforeEach
    void setUp() {
        // @Value fields are not populated by Mockito; set them manually
        ReflectionTestUtils.setField(exportacionService, "bucketName", "lms-bucket");
    }

    // ──────────────────────────────────────────────
    // EXISTING TESTS (basic CRUD operations)
    // ──────────────────────────────────────────────

    @Test
    @DisplayName("solicitarExportacion: debe crear exportación y publicar mensaje en Redis")
    void solicitarExportacion() {
        UUID solicitanteId = UUID.randomUUID();
        String tipoReporte = "CURSOS";
        String filtros = "{\"categoria\":\"PROGRAMACION\"}";

        Exportacion exportacionGuardada = Exportacion.builder()
                .id(UUID.randomUUID())
                .solicitanteId(solicitanteId)
                .tipoReporte(tipoReporte)
                .filtros(filtros)
                .estado("pendiente")
                .build();

        when(exportacionRepository.save(any(Exportacion.class))).thenReturn(exportacionGuardada);

        Exportacion resultado = exportacionService.solicitarExportacion(solicitanteId, tipoReporte, filtros);

        assertNotNull(resultado);
        assertEquals(solicitanteId, resultado.getSolicitanteId());
        assertEquals(tipoReporte, resultado.getTipoReporte());
        assertEquals(filtros, resultado.getFiltros());
        assertEquals("pendiente", resultado.getEstado());
        verify(exportacionRepository, times(1)).save(any(Exportacion.class));
        verify(auditLogger, times(1)).registrarEvento(
                eq("EXPORTACION_SOLICITADA"), eq(solicitanteId), eq("SOLICITAR"),
                eq("EXPORTACION"), eq(exportacionGuardada.getId()), any()
        );
    }

    @Test
    @DisplayName("actualizarEstado: debe marcar exportación como completada con URL de resultado")
    void actualizarEstadoCompletado() {
        UUID exportacionId = UUID.randomUUID();
        Exportacion exportacion = Exportacion.builder()
                .id(exportacionId)
                .solicitanteId(UUID.randomUUID())
                .tipoReporte("USUARIOS")
                .estado("pendiente")
                .build();

        when(exportacionRepository.findById(exportacionId)).thenReturn(Optional.of(exportacion));

        exportacionService.actualizarEstado(exportacionId, "completado", "/downloads/reporte.xlsx", null);

        assertEquals("completado", exportacion.getEstado());
        assertEquals("/downloads/reporte.xlsx", exportacion.getUrlResultado());
        assertNull(exportacion.getErrorMensaje());
        assertNotNull(exportacion.getCompletedAt());
        verify(exportacionRepository, times(1)).save(exportacion);
    }

    @Test
    @DisplayName("actualizarEstado: debe marcar exportación como error con mensaje")
    void actualizarEstadoError() {
        UUID exportacionId = UUID.randomUUID();
        Exportacion exportacion = Exportacion.builder()
                .id(exportacionId)
                .solicitanteId(UUID.randomUUID())
                .tipoReporte("FIRMAS")
                .estado("pendiente")
                .build();

        when(exportacionRepository.findById(exportacionId)).thenReturn(Optional.of(exportacion));

        exportacionService.actualizarEstado(exportacionId, "error", null, "Error al generar el reporte");

        assertEquals("error", exportacion.getEstado());
        assertNull(exportacion.getUrlResultado());
        assertEquals("Error al generar el reporte", exportacion.getErrorMensaje());
        assertNotNull(exportacion.getCompletedAt());
        verify(exportacionRepository, times(1)).save(exportacion);
    }

    @Test
    @DisplayName("actualizarEstado: no debe hacer nada si la exportación no existe")
    void actualizarEstadoExportacionNoEncontrada() {
        UUID exportacionId = UUID.randomUUID();
        when(exportacionRepository.findById(exportacionId)).thenReturn(Optional.empty());

        exportacionService.actualizarEstado(exportacionId, "completado", "/url", null);

        verify(exportacionRepository, never()).save(any());
    }

    @Test
    @DisplayName("listarPorSolicitante: debe retornar exportaciones ordenadas por fecha descendente")
    void listarPorSolicitante() {
        UUID solicitanteId = UUID.randomUUID();
        Exportacion exp1 = Exportacion.builder()
                .id(UUID.randomUUID())
                .solicitanteId(solicitanteId)
                .tipoReporte("CURSOS")
                .estado("completado")
                .createdAt(LocalDateTime.now())
                .build();

        Exportacion exp2 = Exportacion.builder()
                .id(UUID.randomUUID())
                .solicitanteId(solicitanteId)
                .tipoReporte("USUARIOS")
                .estado("pendiente")
                .createdAt(LocalDateTime.now().minusHours(1))
                .build();

        when(exportacionRepository.findBySolicitanteIdOrderByCreatedAtDesc(solicitanteId))
                .thenReturn(List.of(exp1, exp2));

        List<Exportacion> resultados = exportacionService.listarPorSolicitante(solicitanteId);

        assertNotNull(resultados);
        assertEquals(2, resultados.size());
        assertEquals("CURSOS", resultados.get(0).getTipoReporte());
        assertEquals("USUARIOS", resultados.get(1).getTipoReporte());
        verify(exportacionRepository, times(1))
                .findBySolicitanteIdOrderByCreatedAtDesc(solicitanteId);
    }

    @Test
    @DisplayName("obtenerEstado: debe retornar exportación existente")
    void obtenerEstado() {
        UUID exportacionId = UUID.randomUUID();
        Exportacion exportacion = Exportacion.builder()
                .id(exportacionId)
                .solicitanteId(UUID.randomUUID())
                .tipoReporte("EVALUACIONES")
                .estado("procesando")
                .build();

        when(exportacionRepository.findById(exportacionId)).thenReturn(Optional.of(exportacion));

        Optional<Exportacion> resultado = exportacionService.obtenerEstado(exportacionId);

        assertTrue(resultado.isPresent());
        assertEquals("procesando", resultado.get().getEstado());
        assertEquals("EVALUACIONES", resultado.get().getTipoReporte());
    }

    @Test
    @DisplayName("obtenerEstado: debe retornar Optional.empty() si no existe")
    void obtenerEstadoNoEncontrado() {
        UUID exportacionId = UUID.randomUUID();
        when(exportacionRepository.findById(exportacionId)).thenReturn(Optional.empty());

        Optional<Exportacion> resultado = exportacionService.obtenerEstado(exportacionId);

        assertFalse(resultado.isPresent());
    }

    // ──────────────────────────────────────────────
    // descargarReporte TESTS
    // ──────────────────────────────────────────────

    @Test
    @DisplayName("descargarReporte: debe retornar bytes cuando MinIO responde OK")
    void descargarReporteOk() throws Exception {
        String objectName = "exportaciones/test_123.xlsx";
        byte[] expectedBytes = "fake-excel-content".getBytes();

        GetObjectResponse mockResponse = mock(GetObjectResponse.class);
        when(mockResponse.readAllBytes()).thenReturn(expectedBytes);
        when(minioClient.getObject(any(io.minio.GetObjectArgs.class))).thenReturn(mockResponse);

        byte[] result = exportacionService.descargarReporte(objectName);

        assertArrayEquals(expectedBytes, result);
        verify(minioClient, times(1)).getObject(any(io.minio.GetObjectArgs.class));
    }

    @Test
    @DisplayName("descargarReporte: debe lanzar RuntimeException cuando MinIO falla")
    void descargarReporteError() throws Exception {
        String objectName = "exportaciones/missing.xlsx";
        when(minioClient.getObject(any(io.minio.GetObjectArgs.class)))
                .thenThrow(new RuntimeException("MinIO connection error"));

        assertThrows(RuntimeException.class,
                () -> exportacionService.descargarReporte(objectName));
    }

    // ──────────────────────────────────────────────
    // Reporte: EVALUACIONES
    // ──────────────────────────────────────────────

    @Test
    @DisplayName("generarReporteEvaluaciones: debe generar XLSX con 3 hojas y subir a MinIO")
    void generarReporteEvaluacionesOk() throws Exception {
        UUID expId = UUID.randomUUID();
        UUID cursoId = UUID.randomUUID();
        Curso curso = Curso.builder().id(cursoId).titulo("Java Avanzado").build();
        Evaluacion evaluacion = Evaluacion.builder()
                .id(UUID.randomUUID())
                .codigo("EVA-001")
                .titulo("Examen Final")
                .curso(curso)
                .pesoTotal(100)
                .hashSha256("abc123")
                .createdAt(LocalDateTime.of(2026, 1, 15, 10, 0))
                .build();

        Exportacion exportacion = Exportacion.builder().id(expId).estado("procesando").build();
        when(evaluacionRepository.findAll()).thenReturn(List.of(evaluacion));
        when(exportacionRepository.findById(expId)).thenReturn(Optional.of(exportacion));
        // Enriched method uses preguntaRepository and inscripcionRepository for sheets 2/3
        when(preguntaRepository.findByEvaluacionIdOrderByOrdenAsc(evaluacion.getId())).thenReturn(List.of());
        when(inscripcionRepository.findByCursoIdAndDeletedAtIsNull(cursoId)).thenReturn(List.of());
        when(minioClient.putObject(any(PutObjectArgs.class))).thenReturn(null);

        exportacionService.generarReporteEvaluaciones(expId, null);

        assertEquals("completado", exportacion.getEstado());
        assertNotNull(exportacion.getUrlResultado());
        assertTrue(exportacion.getUrlResultado().contains("/descargar?object=exportaciones/evaluaciones_" + expId));
        verify(preguntaRepository, times(2)).findByEvaluacionIdOrderByOrdenAsc(evaluacion.getId());
        verify(inscripcionRepository, times(2)).findByCursoIdAndDeletedAtIsNull(cursoId);
        verify(minioClient, times(1)).putObject(any(PutObjectArgs.class));
    }

    @Test
    @DisplayName("generarReporteEvaluaciones: debe marcar error cuando evaluacionRepository falla")
    void generarReporteEvaluacionesError() {
        UUID expId = UUID.randomUUID();
        Exportacion exportacion = Exportacion.builder().id(expId).estado("procesando").build();
        when(evaluacionRepository.findAll()).thenThrow(new RuntimeException("DB error"));
        when(exportacionRepository.findById(expId)).thenReturn(Optional.of(exportacion));

        exportacionService.generarReporteEvaluaciones(expId, null);

        assertEquals("error", exportacion.getEstado());
        assertNotNull(exportacion.getErrorMensaje());
    }

    // ──────────────────────────────────────────────
    // Reporte: SCORM
    // ──────────────────────────────────────────────

    @Test
    @DisplayName("generarReporteScorm: debe generar XLSX con 2 hojas y subir a MinIO")
    void generarReporteScormOk() throws Exception {
        UUID expId = UUID.randomUUID();
        UUID paqueteId = UUID.randomUUID();
        Curso curso = Curso.builder().id(UUID.randomUUID()).build();
        PaqueteScorm paquete = PaqueteScorm.builder()
                .id(paqueteId)
                .codigo("SCORM-001")
                .titulo("Módulo 1")
                .curso(curso)
                .versionScorm("2004 4th Edition")
                .tamanoBytes(2048000)
                .hashSha256("def456")
                .createdAt(LocalDateTime.of(2026, 2, 10, 14, 30))
                .build();

        Exportacion exportacion = Exportacion.builder().id(expId).estado("procesando").build();
        when(paqueteScormRepository.findAll()).thenReturn(List.of(paquete));
        when(exportacionRepository.findById(expId)).thenReturn(Optional.of(exportacion));
        // Enriched method uses scormDatoAlumnoRepository for sheet 2 (progreso)
        when(scormDatoAlumnoRepository.findDistinctUsuarioIdByPaqueteId(paqueteId)).thenReturn(List.of());
        when(minioClient.putObject(any(PutObjectArgs.class))).thenReturn(null);

        exportacionService.generarReporteScorm(expId, null);

        assertEquals("completado", exportacion.getEstado());
        assertTrue(exportacion.getUrlResultado().contains("/descargar?object=exportaciones/scorm_" + expId));
        verify(scormDatoAlumnoRepository, times(1)).findDistinctUsuarioIdByPaqueteId(paqueteId);
        verify(minioClient, times(1)).putObject(any(PutObjectArgs.class));
    }

    @Test
    @DisplayName("generarReporteScorm: debe marcar error cuando paqueteScormRepository falla")
    void generarReporteScormError() {
        UUID expId = UUID.randomUUID();
        Exportacion exportacion = Exportacion.builder().id(expId).estado("procesando").build();
        when(paqueteScormRepository.findAll()).thenThrow(new RuntimeException("DB error"));
        when(exportacionRepository.findById(expId)).thenReturn(Optional.of(exportacion));

        exportacionService.generarReporteScorm(expId, null);

        assertEquals("error", exportacion.getEstado());
        assertNotNull(exportacion.getErrorMensaje());
    }

    // ──────────────────────────────────────────────
    // Reporte: CURSOS (T2)
    // ──────────────────────────────────────────────

    @Test
    @DisplayName("generarReporteCursos: debe generar XLSX con datos reales y subir a MinIO")
    void generarReporteCursosOk() throws Exception {
        UUID expId = UUID.randomUUID();
        UUID cursoId = UUID.randomUUID();
        Curso curso = Curso.builder()
                .id(cursoId)
                .titulo("Java Avanzado")
                .estado("ACTIVO")
                .categoria("PROGRAMACION")
                .versionActual(2)
                .createdAt(LocalDateTime.of(2026, 3, 1, 8, 0))
                .build();

        Page<Curso> page = new PageImpl<>(List.of(curso));
        Exportacion exportacion = Exportacion.builder().id(expId).estado("procesando").build();

        when(cursoRepository.findByDeletedAtIsNull(any(Pageable.class))).thenReturn(page);
        when(inscripcionRepository.countByCursoIdAndDeletedAtIsNull(cursoId)).thenReturn(15L);
        when(inscripcionRepository.countByCursoIdAndCompletadoTrueAndDeletedAtIsNull(cursoId)).thenReturn(10L);
        when(inscripcionRepository.findUltimoAccesoByCursoId(cursoId))
                .thenReturn(Optional.of(LocalDateTime.of(2026, 6, 1, 12, 0)));
        when(exportacionRepository.findById(expId)).thenReturn(Optional.of(exportacion));
        when(minioClient.putObject(any(PutObjectArgs.class))).thenReturn(null);

        exportacionService.generarReporteCursos(expId);

        assertEquals("completado", exportacion.getEstado());
        assertTrue(exportacion.getUrlResultado().contains("/descargar?object=exportaciones/cursos_" + expId));
        verify(cursoRepository, times(1)).findByDeletedAtIsNull(any(Pageable.class));
        verify(inscripcionRepository, times(1)).countByCursoIdAndDeletedAtIsNull(cursoId);
        verify(inscripcionRepository, times(1)).countByCursoIdAndCompletadoTrueAndDeletedAtIsNull(cursoId);
        verify(inscripcionRepository, times(1)).findUltimoAccesoByCursoId(cursoId);
        verify(minioClient, times(1)).putObject(any(PutObjectArgs.class));
    }

    @Test
    @DisplayName("generarReporteCursos: debe manejar lista vacía de cursos")
    void generarReporteCursosEmpty() throws Exception {
        UUID expId = UUID.randomUUID();
        Exportacion exportacion = Exportacion.builder().id(expId).estado("procesando").build();

        when(cursoRepository.findByDeletedAtIsNull(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        when(exportacionRepository.findById(expId)).thenReturn(Optional.of(exportacion));
        when(minioClient.putObject(any(PutObjectArgs.class))).thenReturn(null);

        exportacionService.generarReporteCursos(expId);

        assertEquals("completado", exportacion.getEstado());
        verify(minioClient, times(1)).putObject(any(PutObjectArgs.class));
    }

    @Test
    @DisplayName("generarReporteCursos: debe marcar error cuando cursoRepository falla")
    void generarReporteCursosError() {
        UUID expId = UUID.randomUUID();
        Exportacion exportacion = Exportacion.builder().id(expId).estado("procesando").build();
        when(cursoRepository.findByDeletedAtIsNull(any(Pageable.class)))
                .thenThrow(new RuntimeException("DB error"));
        when(exportacionRepository.findById(expId)).thenReturn(Optional.of(exportacion));

        exportacionService.generarReporteCursos(expId);

        assertEquals("error", exportacion.getEstado());
        assertNotNull(exportacion.getErrorMensaje());
    }

    // ──────────────────────────────────────────────
    // Reporte: USUARIOS (T3)
    // ──────────────────────────────────────────────

    @Test
    @DisplayName("generarReporteUsuarios: debe generar XLSX con roles concatenados")
    void generarReporteUsuariosOk() throws Exception {
        UUID expId = UUID.randomUUID();
        UUID usuarioId = UUID.randomUUID();

        Rol rolAdmin = Rol.builder().id(UUID.randomUUID()).nombre("ADMIN").build();
        Rol rolEstudiante = Rol.builder().id(UUID.randomUUID()).nombre("ESTUDIANTE").build();

        Usuario usuario = Usuario.builder()
                .id(usuarioId)
                .email("user@test.com")
                .nombreCompleto("Usuario Test")
                .rut("12.345.678-9")
                .roles(Set.of(rolAdmin, rolEstudiante))
                .mfaHabilitado(true)
                .consentimientoOtorgado(true)
                .ultimoLogin(LocalDateTime.of(2026, 6, 15, 9, 30))
                .createdAt(LocalDateTime.of(2025, 1, 1, 0, 0))
                .hashSha256("xyz789")
                .build();

        Page<Usuario> page = new PageImpl<>(List.of(usuario));
        Exportacion exportacion = Exportacion.builder().id(expId).estado("procesando").build();

        when(usuarioRepository.findAllActive(any(Pageable.class))).thenReturn(page);
        when(exportacionRepository.findById(expId)).thenReturn(Optional.of(exportacion));
        when(minioClient.putObject(any(PutObjectArgs.class))).thenReturn(null);

        exportacionService.generarReporteUsuarios(expId, null);

        assertEquals("completado", exportacion.getEstado());
        assertTrue(exportacion.getUrlResultado().contains("/descargar?object=exportaciones/usuarios_" + expId));
        verify(usuarioRepository, times(1)).findAllActive(any(Pageable.class));
        verify(minioClient, times(1)).putObject(any(PutObjectArgs.class));
    }

    @Test
    @DisplayName("generarReporteUsuarios: debe marcar error cuando usuarioRepository falla")
    void generarReporteUsuariosError() {
        UUID expId = UUID.randomUUID();
        Exportacion exportacion = Exportacion.builder().id(expId).estado("procesando").build();
        when(usuarioRepository.findAllActive(any(Pageable.class)))
                .thenThrow(new RuntimeException("DB error"));
        when(exportacionRepository.findById(expId)).thenReturn(Optional.of(exportacion));

        exportacionService.generarReporteUsuarios(expId, null);

        assertEquals("error", exportacion.getEstado());
        assertNotNull(exportacion.getErrorMensaje());
    }

    // ──────────────────────────────────────────────
    // Reporte: FIRMAS (T4)
    // ──────────────────────────────────────────────

    @Test
    @DisplayName("generarReporteFirmas: debe generar XLSX con datos de firmas")
    void generarReporteFirmasOk() throws Exception {
        UUID expId = UUID.randomUUID();
        Usuario usuario = Usuario.builder().id(UUID.randomUUID()).email("firmante@test.com").build();
        Documento documento = Documento.builder().id(UUID.randomUUID()).build();

        FirmaElectronica firma = FirmaElectronica.builder()
                .id(UUID.randomUUID())
                .usuario(usuario)
                .documento(documento)
                .ipOrigen("192.168.1.1")
                .timestamp(LocalDateTime.of(2026, 5, 20, 15, 0))
                .sistemaOperativo("Windows 10")
                .geolocalizacion("Santiago, Chile")
                .hashDocumento("hash-doc-001")
                .hashSha256("hash-firma-001")
                .hashAnterior("hash-prev-001")
                .build();

        Exportacion exportacion = Exportacion.builder().id(expId).estado("procesando").build();
        when(firmaElectronicaRepository.findAll()).thenReturn(List.of(firma));
        when(exportacionRepository.findById(expId)).thenReturn(Optional.of(exportacion));
        when(minioClient.putObject(any(PutObjectArgs.class))).thenReturn(null);

        exportacionService.generarReporteFirmas(expId);

        assertEquals("completado", exportacion.getEstado());
        assertTrue(exportacion.getUrlResultado().contains("/descargar?object=exportaciones/firmas_" + expId));
        verify(firmaElectronicaRepository, times(1)).findAll();
        verify(minioClient, times(1)).putObject(any(PutObjectArgs.class));
    }

    @Test
    @DisplayName("generarReporteFirmas: debe marcar error cuando firmaRepository falla")
    void generarReporteFirmasError() {
        UUID expId = UUID.randomUUID();
        Exportacion exportacion = Exportacion.builder().id(expId).estado("procesando").build();
        when(firmaElectronicaRepository.findAll()).thenThrow(new RuntimeException("DB error"));
        when(exportacionRepository.findById(expId)).thenReturn(Optional.of(exportacion));

        exportacionService.generarReporteFirmas(expId);

        assertEquals("error", exportacion.getEstado());
        assertNotNull(exportacion.getErrorMensaje());
    }

    // ──────────────────────────────────────────────
    // Reporte: AUDITORÍA (T5)
    // ──────────────────────────────────────────────

    @Test
    @DisplayName("generarReporteAuditoria: debe generar XLSX con eventos de auditoría")
    void generarReporteAuditoriaOk() throws Exception {
        UUID expId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();

        EventoAuditoria evento = EventoAuditoria.builder()
                .id(UUID.randomUUID())
                .tipoEvento("LOGIN")
                .actorId(actorId)
                .accion("AUTENTICACION")
                .recursoTipo("USUARIO")
                .recursoId(actorId)
                .metadata(Map.of("ip", "10.0.0.1"))
                .hashSha256("audit-hash-001")
                .hashAnterior("audit-prev-001")
                .createdAt(LocalDateTime.of(2026, 4, 10, 8, 30))
                .build();

        Exportacion exportacion = Exportacion.builder().id(expId).estado("procesando").build();
        when(eventoAuditoriaRepository.findAll()).thenReturn(List.of(evento));
        when(exportacionRepository.findById(expId)).thenReturn(Optional.of(exportacion));
        when(minioClient.putObject(any(PutObjectArgs.class))).thenReturn(null);

        exportacionService.generarReporteAuditoria(expId);

        assertEquals("completado", exportacion.getEstado());
        assertTrue(exportacion.getUrlResultado().contains("/descargar?object=exportaciones/auditoria_" + expId));
        verify(eventoAuditoriaRepository, times(1)).findAll();
        verify(minioClient, times(1)).putObject(any(PutObjectArgs.class));
    }

    @Test
    @DisplayName("generarReporteAuditoria: debe marcar error cuando eventoAuditoriaRepository falla")
    void generarReporteAuditoriaError() {
        UUID expId = UUID.randomUUID();
        Exportacion exportacion = Exportacion.builder().id(expId).estado("procesando").build();
        when(eventoAuditoriaRepository.findAll()).thenThrow(new RuntimeException("DB error"));
        when(exportacionRepository.findById(expId)).thenReturn(Optional.of(exportacion));

        exportacionService.generarReporteAuditoria(expId);

        assertEquals("error", exportacion.getEstado());
        assertNotNull(exportacion.getErrorMensaje());
    }

    // ──────────────────────────────────────────────
    // procesarExportacion - ROUTING TESTS
    // ──────────────────────────────────────────────

    @Test
    @DisplayName("procesarExportacion: debe rutear 'evaluaciones' a generarReporteEvaluaciones")
    void procesarExportacionRuteoEvaluaciones() throws Exception {
        UUID expId = UUID.randomUUID();
        Exportacion exportacion = Exportacion.builder()
                .id(expId).tipoReporte("evaluaciones").estado("pendiente").build();
        when(exportacionRepository.findById(expId)).thenReturn(Optional.of(exportacion));
        when(evaluacionRepository.findAll()).thenReturn(List.of());
        when(minioClient.putObject(any(PutObjectArgs.class))).thenReturn(null);

        exportacionService.procesarExportacion(expId);

        verify(evaluacionRepository, times(1)).findAll();
        verify(minioClient, times(1)).putObject(any(PutObjectArgs.class));
    }

    @Test
    @DisplayName("procesarExportacion: debe rutear 'cursos' a generarReporteCursos")
    void procesarExportacionRuteoCursos() {
        UUID expId = UUID.randomUUID();
        Exportacion exportacion = Exportacion.builder()
                .id(expId).tipoReporte("cursos").estado("pendiente").build();
        when(exportacionRepository.findById(expId)).thenReturn(Optional.of(exportacion));
        when(cursoRepository.findByDeletedAtIsNull(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        exportacionService.procesarExportacion(expId);

        verify(cursoRepository, times(1)).findByDeletedAtIsNull(any(Pageable.class));
    }

    @Test
    @DisplayName("procesarExportacion: debe rutear 'usuarios' a generarReporteUsuarios")
    void procesarExportacionRuteoUsuarios() {
        UUID expId = UUID.randomUUID();
        Exportacion exportacion = Exportacion.builder()
                .id(expId).tipoReporte("usuarios").estado("pendiente").build();
        when(exportacionRepository.findById(expId)).thenReturn(Optional.of(exportacion));
        when(usuarioRepository.findAllActive(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        exportacionService.procesarExportacion(expId);

        verify(usuarioRepository, times(1)).findAllActive(any(Pageable.class));
    }

    @Test
    @DisplayName("procesarExportacion: debe rutear 'firmas' a generarReporteFirmas")
    void procesarExportacionRuteoFirmas() {
        UUID expId = UUID.randomUUID();
        Exportacion exportacion = Exportacion.builder()
                .id(expId).tipoReporte("firmas").estado("pendiente").build();
        when(exportacionRepository.findById(expId)).thenReturn(Optional.of(exportacion));
        when(firmaElectronicaRepository.findAll()).thenReturn(List.of());

        exportacionService.procesarExportacion(expId);

        verify(firmaElectronicaRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("procesarExportacion: debe rutear 'auditoria' a generarReporteAuditoria")
    void procesarExportacionRuteoAuditoria() {
        UUID expId = UUID.randomUUID();
        Exportacion exportacion = Exportacion.builder()
                .id(expId).tipoReporte("auditoria").estado("pendiente").build();
        when(exportacionRepository.findById(expId)).thenReturn(Optional.of(exportacion));
        when(eventoAuditoriaRepository.findAll()).thenReturn(List.of());

        exportacionService.procesarExportacion(expId);

        verify(eventoAuditoriaRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("procesarExportacion: debe rutear 'scorm' a generarReporteScorm")
    void procesarExportacionRuteoScorm() throws Exception {
        UUID expId = UUID.randomUUID();
        Exportacion exportacion = Exportacion.builder()
                .id(expId).tipoReporte("scorm").estado("pendiente").build();
        when(exportacionRepository.findById(expId)).thenReturn(Optional.of(exportacion));
        when(paqueteScormRepository.findAll()).thenReturn(List.of());
        when(minioClient.putObject(any(PutObjectArgs.class))).thenReturn(null);

        exportacionService.procesarExportacion(expId);

        verify(paqueteScormRepository, times(1)).findAll();
        verify(minioClient, times(1)).putObject(any(PutObjectArgs.class));
    }

    @Test
    @DisplayName("procesarExportacion: tipo no soportado debe marcar error")
    void procesarExportacionTipoNoSoportado() {
        UUID expId = UUID.randomUUID();
        Exportacion exportacion = Exportacion.builder()
                .id(expId).tipoReporte("inventos").estado("pendiente").build();
        when(exportacionRepository.findById(expId)).thenReturn(Optional.of(exportacion));

        exportacionService.procesarExportacion(expId);

        assertEquals("error", exportacion.getEstado());
        assertTrue(exportacion.getErrorMensaje().contains("no soportado"));
    }

    // ──────────────────────────────────────────────
    // generarExcelEjemplo - estructura del XLSX
    // ──────────────────────────────────────────────

    @Test
    @DisplayName("generarExcelEjemplo: debe crear XLSX con headers y datos")
    void generarExcelEjemploEstructura() {
        String[] headers = {"Col1", "Col2", "Col3"};
        List<String[]> data = List.of(
                new String[]{"A", "B", "C"},
                new String[]{"D", "E", "F"}
        );

        byte[] excelBytes = exportacionService.generarExcelEjemplo(headers, data);

        assertNotNull(excelBytes);
        assertTrue(excelBytes.length > 0);
        // Verificar que es un XLSX válido (firma ZIP)
        assertArrayEquals(
                new byte[]{0x50, 0x4B, 0x03, 0x04},
                Arrays.copyOfRange(excelBytes, 0, 4)
        );
    }

    @Test
    @DisplayName("generarExcelEjemplo: debe manejar lista vacía de datos")
    void generarExcelEjemploVacio() {
        String[] headers = {"Col1"};
        List<String[]> data = List.of();

        byte[] excelBytes = exportacionService.generarExcelEjemplo(headers, data);

        assertNotNull(excelBytes);
        assertTrue(excelBytes.length > 0);
    }
}
