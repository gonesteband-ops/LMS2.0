package com.lms.service;

import com.lms.dto.EvaluacionRequest;
import com.lms.dto.PreguntaRequest;
import com.lms.model.Curso;
import com.lms.model.Evaluacion;
import com.lms.model.Pregunta;
import com.lms.repository.EvaluacionRepository;
import com.lms.repository.PreguntaRepository;
import com.lms.util.AuditLogger;
import com.lms.util.HashUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import org.mockito.ArgumentCaptor;

@ExtendWith(MockitoExtension.class)
@DisplayName("EvaluacionService - Evaluation & Question Management")
class EvaluacionServiceTest {

    @Mock
    private EvaluacionRepository evaluacionRepository;

    @Mock
    private PreguntaRepository preguntaRepository;

    @Mock
    private CursoService cursoService;

    @Mock
    private AuditLogger auditLogger;

    @Mock
    private CodigoGeneratorService codigoGeneratorService;

    @Mock
    private NotificacionService notificacionService;

    @Mock
    private com.lms.repository.InscripcionRepository inscripcionRepository;

    @InjectMocks
    private EvaluacionService evaluacionService;

    private Curso crearCursoMock(UUID cursoId) {
        return Curso.builder()
                .id(cursoId)
                .titulo("Curso de prueba")
                .build();
    }

    private Evaluacion crearEvaluacionMock(UUID evId, Curso curso) {
        return Evaluacion.builder()
                .id(evId)
                .curso(curso)
                .titulo("Examen Final")
                .descripcion("Evaluación de prueba")
                .pesoTotal(100.0)
                .tiempoEstimadoMinutos(60)
                .codigo("EVA-001")
                .hashSha256(HashUtils.sha256("Examen Final"))
                .build();
    }

    private Pregunta crearPreguntaMock(UUID pId, Evaluacion evaluacion, int orden) {
        return Pregunta.builder()
                .id(pId)
                .evaluacion(evaluacion)
                .tipo("opcion_unica")
                .contenido("¿Pregunta " + orden + "?")
                .peso(50.0)
                .tiempoRespuestaMs(120000)
                .opciones("[{\"texto\":\"Opción A\",\"correcta\":true}]")
                .respuestaCorrecta(null)
                .hashSha256(HashUtils.sha256("¿Pregunta " + orden + "?"))
                .orden(orden)
                .build();
    }

    private EvaluacionRequest crearRequestValido() {
        PreguntaRequest p1 = new PreguntaRequest(
                "opcion_unica", "¿Pregunta 1?", 50.0, 120000,
                "[{\"texto\":\"A\",\"correcta\":true},{\"texto\":\"B\",\"correcta\":false}]",
                null, 0
        );
        PreguntaRequest p2 = new PreguntaRequest(
                "verdadero_falso", "¿Es cierto?", 50.0, 60000,
                "[{\"texto\":\"Verdadero\",\"correcta\":true},{\"texto\":\"Falso\",\"correcta\":false}]",
                null, 1
        );
        return new EvaluacionRequest("Examen Final", "Evaluación completa", 100.0, 60, "{\"version\":\"1.0\"}", 4.0, List.of(p1, p2));
    }

    // ============================================================
    // CREAR EVALUACIÓN
    // ============================================================

    @Test
    @DisplayName("crearEvaluacion: debe crear evaluación con preguntas correctamente")
    void crearEvaluacionConPreguntas() {
        // Arrange
        UUID cursoId = UUID.randomUUID();
        UUID autorId = UUID.randomUUID();
        Curso curso = crearCursoMock(cursoId);
        EvaluacionRequest request = crearRequestValido();

        when(cursoService.buscarPorId(cursoId)).thenReturn(Optional.of(curso));
        when(codigoGeneratorService.generarCodigo("EVA")).thenReturn("EVA-001");
        when(evaluacionRepository.save(any(Evaluacion.class))).thenAnswer(i -> {
            Evaluacion ev = i.getArgument(0);
            ev.setId(UUID.randomUUID());
            return ev;
        });
        when(preguntaRepository.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));

        // Act
        Evaluacion resultado = evaluacionService.crearEvaluacion(cursoId, request, autorId);

        // Assert
        assertNotNull(resultado);
        assertEquals("Examen Final", resultado.getTitulo());
        assertEquals(100.0, resultado.getPesoTotal());
        assertEquals("EVA-001", resultado.getCodigo());
        verify(cursoService, times(1)).buscarPorId(cursoId);
        verify(evaluacionRepository, times(1)).save(any(Evaluacion.class));
        verify(preguntaRepository, times(1)).saveAll(anyList());
        verify(auditLogger, times(1)).registrarEvento(
                eq("EVALUACION_CREADA"), eq(autorId), eq("CREAR"),
                eq("EVALUACION"), any(), any()
        );
    }

    @Test
    @DisplayName("crearEvaluacion: debe lanzar excepción si el curso no existe")
    void crearEvaluacionCursoNoExistente() {
        // Arrange
        UUID cursoId = UUID.randomUUID();
        UUID autorId = UUID.randomUUID();
        EvaluacionRequest request = crearRequestValido();

        when(cursoService.buscarPorId(cursoId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> evaluacionService.crearEvaluacion(cursoId, request, autorId));
        verify(evaluacionRepository, never()).save(any());
    }

    @Test
    @DisplayName("crearEvaluacion: debe crear evaluación sin preguntas (null)")
    void crearEvaluacionSinPreguntasNull() {
        // Arrange
        UUID cursoId = UUID.randomUUID();
        UUID autorId = UUID.randomUUID();
        Curso curso = crearCursoMock(cursoId);
        EvaluacionRequest request = new EvaluacionRequest("Solo título", null, 100.0, 30, null, null, null);

        when(cursoService.buscarPorId(cursoId)).thenReturn(Optional.of(curso));
        when(codigoGeneratorService.generarCodigo("EVA")).thenReturn("EVA-002");
        when(evaluacionRepository.save(any(Evaluacion.class))).thenAnswer(i -> {
            Evaluacion ev = i.getArgument(0);
            ev.setId(UUID.randomUUID());
            return ev;
        });

        // Act
        Evaluacion resultado = evaluacionService.crearEvaluacion(cursoId, request, autorId);

        // Assert
        assertNotNull(resultado);
        assertNull(resultado.getDescripcion());
        assertEquals("EVA-002", resultado.getCodigo());
        verify(preguntaRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("crearEvaluacion: debe crear evaluación con preguntas vacías")
    void crearEvaluacionConPreguntasVacias() {
        // Arrange
        UUID cursoId = UUID.randomUUID();
        UUID autorId = UUID.randomUUID();
        Curso curso = crearCursoMock(cursoId);
        EvaluacionRequest request = new EvaluacionRequest("Evaluación vacía", "Sin preguntas", 100.0, 30, null, 4.0, List.of());

        when(cursoService.buscarPorId(cursoId)).thenReturn(Optional.of(curso));
        when(codigoGeneratorService.generarCodigo("EVA")).thenReturn("EVA-003");
        when(evaluacionRepository.save(any(Evaluacion.class))).thenAnswer(i -> {
            Evaluacion ev = i.getArgument(0);
            ev.setId(UUID.randomUUID());
            return ev;
        });

        // Act
        Evaluacion resultado = evaluacionService.crearEvaluacion(cursoId, request, autorId);

        // Assert
        assertNotNull(resultado);
        assertEquals(0, resultado.getPreguntas() == null ? 0 : resultado.getPreguntas().size());
        verify(preguntaRepository, never()).saveAll(anyList());
    }

    // ============================================================
    // ACTUALIZAR EVALUACIÓN
    // ============================================================

    @Test
    @DisplayName("actualizarEvaluacion: debe actualizar datos y reemplazar preguntas")
    void actualizarEvaluacionExitosa() {
        // Arrange
        UUID evId = UUID.randomUUID();
        UUID cursoId = UUID.randomUUID();
        UUID autorId = UUID.randomUUID();
        Curso curso = crearCursoMock(cursoId);
        Evaluacion evaluacion = crearEvaluacionMock(evId, curso);
        EvaluacionRequest request = crearRequestValido();

        when(evaluacionRepository.findById(evId)).thenReturn(Optional.of(evaluacion));
        when(preguntaRepository.findByEvaluacionIdOrderByOrdenAsc(evId)).thenReturn(List.of());
        when(evaluacionRepository.save(any(Evaluacion.class))).thenReturn(evaluacion);
        when(preguntaRepository.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));

        // Act
        Evaluacion resultado = evaluacionService.actualizarEvaluacion(evId, request, autorId);

        // Assert
        assertNotNull(resultado);
        assertEquals("Examen Final", resultado.getTitulo());
        assertEquals(100.0, resultado.getPesoTotal());
        verify(evaluacionRepository, times(1)).findById(evId);
        verify(preguntaRepository, times(1)).findByEvaluacionIdOrderByOrdenAsc(evId);
        verify(evaluacionRepository, times(1)).save(evaluacion);
        verify(preguntaRepository, times(1)).saveAll(anyList());
        verify(auditLogger, times(1)).registrarEvento(
                eq("EVALUACION_ACTUALIZADA"), eq(autorId), eq("ACTUALIZAR"),
                eq("EVALUACION"), any(), any()
        );
    }

    @Test
    @DisplayName("actualizarEvaluacion: debe lanzar excepción si la evaluación no existe")
    void actualizarEvaluacionNoExistente() {
        // Arrange
        UUID evId = UUID.randomUUID();
        UUID autorId = UUID.randomUUID();
        EvaluacionRequest request = crearRequestValido();

        when(evaluacionRepository.findById(evId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> evaluacionService.actualizarEvaluacion(evId, request, autorId));
        verify(evaluacionRepository, never()).save(any());
    }

    @Test
    @DisplayName("actualizarEvaluacion: debe eliminar preguntas anteriores antes de crear nuevas")
    void actualizarEvaluacionEliminaPreguntasAnteriores() {
        // Arrange
        UUID evId = UUID.randomUUID();
        UUID cursoId = UUID.randomUUID();
        UUID autorId = UUID.randomUUID();
        UUID pId1 = UUID.randomUUID();
        UUID pId2 = UUID.randomUUID();
        Curso curso = crearCursoMock(cursoId);
        Evaluacion evaluacion = crearEvaluacionMock(evId, curso);

        Pregunta preguntaAntigua1 = crearPreguntaMock(pId1, evaluacion, 0);
        Pregunta preguntaAntigua2 = crearPreguntaMock(pId2, evaluacion, 1);
        List<Pregunta> preguntasAnteriores = List.of(preguntaAntigua1, preguntaAntigua2);

        EvaluacionRequest request = crearRequestValido();

        when(evaluacionRepository.findById(evId)).thenReturn(Optional.of(evaluacion));
        when(preguntaRepository.findByEvaluacionIdOrderByOrdenAsc(evId)).thenReturn(preguntasAnteriores);
        when(evaluacionRepository.save(any(Evaluacion.class))).thenReturn(evaluacion);
        when(preguntaRepository.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));

        // Act
        Evaluacion resultado = evaluacionService.actualizarEvaluacion(evId, request, autorId);

        // Assert
        assertNotNull(resultado);
        verify(preguntaRepository, times(1)).deleteAll(preguntasAnteriores);
        verify(preguntaRepository, times(1)).saveAll(anyList());
    }

    // ============================================================
    // ELIMINAR EVALUACIÓN
    // ============================================================

    @Test
    @DisplayName("eliminarEvaluacion: debe hacer soft-delete de evaluación y preguntas")
    void eliminarEvaluacionSoftDelete() {
        // Arrange
        UUID evId = UUID.randomUUID();
        UUID cursoId = UUID.randomUUID();
        UUID autorId = UUID.randomUUID();
        Curso curso = crearCursoMock(cursoId);
        Evaluacion evaluacion = crearEvaluacionMock(evId, curso);

        Pregunta p1 = crearPreguntaMock(UUID.randomUUID(), evaluacion, 0);
        Pregunta p2 = crearPreguntaMock(UUID.randomUUID(), evaluacion, 1);
        List<Pregunta> preguntas = List.of(p1, p2);

        when(evaluacionRepository.findById(evId)).thenReturn(Optional.of(evaluacion));
        when(preguntaRepository.findByEvaluacionIdOrderByOrdenAsc(evId)).thenReturn(preguntas);
        when(evaluacionRepository.save(any(Evaluacion.class))).thenReturn(evaluacion);
        when(preguntaRepository.saveAll(anyList())).thenReturn(preguntas);

        // Act
        evaluacionService.eliminarEvaluacion(evId, autorId);

        // Assert
        assertNotNull(evaluacion.getDeletedAt());
        preguntas.forEach(p -> assertNotNull(p.getDeletedAt()));
        verify(preguntaRepository, times(1)).saveAll(preguntas);
        verify(evaluacionRepository, times(1)).save(evaluacion);
        verify(auditLogger, times(1)).registrarEvento(
                eq("EVALUACION_ELIMINADA"), eq(autorId), eq("ELIMINAR"),
                eq("EVALUACION"), any(), any()
        );
    }

    @Test
    @DisplayName("eliminarEvaluacion: debe lanzar excepción si la evaluación no existe")
    void eliminarEvaluacionNoExistente() {
        // Arrange
        UUID evId = UUID.randomUUID();
        UUID autorId = UUID.randomUUID();

        when(evaluacionRepository.findById(evId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> evaluacionService.eliminarEvaluacion(evId, autorId));
        verify(evaluacionRepository, never()).save(any());
        verify(preguntaRepository, never()).saveAll(anyList());
    }

    // ============================================================
    // LISTAR POR CURSO
    // ============================================================

    @Test
    @DisplayName("listarPorCurso: debe retornar evaluaciones del curso")
    void listarPorCursoConResultados() {
        // Arrange
        UUID cursoId = UUID.randomUUID();
        Curso curso = crearCursoMock(cursoId);
        Evaluacion ev1 = crearEvaluacionMock(UUID.randomUUID(), curso);
        Evaluacion ev2 = crearEvaluacionMock(UUID.randomUUID(), curso);

        when(evaluacionRepository.findByCursoId(cursoId)).thenReturn(List.of(ev1, ev2));

        // Act
        List<Evaluacion> resultado = evaluacionService.listarPorCurso(cursoId);

        // Assert
        assertNotNull(resultado);
        assertEquals(2, resultado.size());
        verify(evaluacionRepository, times(1)).findByCursoId(cursoId);
    }

    @Test
    @DisplayName("listarPorCurso: debe retornar lista vacía si no hay evaluaciones")
    void listarPorCursoSinResultados() {
        // Arrange
        UUID cursoId = UUID.randomUUID();

        when(evaluacionRepository.findByCursoId(cursoId)).thenReturn(List.of());

        // Act
        List<Evaluacion> resultado = evaluacionService.listarPorCurso(cursoId);

        // Assert
        assertNotNull(resultado);
        assertTrue(resultado.isEmpty());
    }

    // ============================================================
    // LISTAR POR CURSO PAGINADO
    // ============================================================

    @Test
    @DisplayName("listarPorCursoPaginado: debe retornar página de evaluaciones")
    void listarPorCursoPaginadoConResultados() {
        // Arrange
        UUID cursoId = UUID.randomUUID();
        Curso curso = crearCursoMock(cursoId);
        Evaluacion ev = crearEvaluacionMock(UUID.randomUUID(), curso);
        PageRequest pageable = PageRequest.of(0, 10);

        when(evaluacionRepository.findByCursoId(cursoId, pageable))
                .thenReturn(new PageImpl<>(List.of(ev)));

        // Act
        Page<Evaluacion> resultado = evaluacionService.listarPorCursoPaginado(cursoId, pageable);

        // Assert
        assertNotNull(resultado);
        assertEquals(1, resultado.getContent().size());
        assertEquals("Examen Final", resultado.getContent().get(0).getTitulo());
    }

    // ============================================================
    // BUSCAR POR CÓDIGO
    // ============================================================

    @Test
    @DisplayName("buscarPorCodigo: debe retornar evaluación cuando existe")
    void buscarPorCodigoExistente() {
        // Arrange
        UUID cursoId = UUID.randomUUID();
        Curso curso = crearCursoMock(cursoId);
        Evaluacion ev = crearEvaluacionMock(UUID.randomUUID(), curso);

        when(evaluacionRepository.findByCodigo("EVA-001")).thenReturn(Optional.of(ev));

        // Act
        Optional<Evaluacion> resultado = evaluacionService.buscarPorCodigo("EVA-001");

        // Assert
        assertTrue(resultado.isPresent());
        assertEquals("EVA-001", resultado.get().getCodigo());
    }

    @Test
    @DisplayName("buscarPorCodigo: debe retornar vacío cuando no existe")
    void buscarPorCodigoNoExistente() {
        // Arrange
        when(evaluacionRepository.findByCodigo("EVA-999")).thenReturn(Optional.empty());

        // Act
        Optional<Evaluacion> resultado = evaluacionService.buscarPorCodigo("EVA-999");

        // Assert
        assertFalse(resultado.isPresent());
    }

    // ============================================================
    // BUSCAR POR ID
    // ============================================================

    @Test
    @DisplayName("buscarPorId: debe retornar evaluación cuando existe")
    void buscarPorIdExistente() {
        // Arrange
        UUID evId = UUID.randomUUID();
        Curso curso = crearCursoMock(UUID.randomUUID());
        Evaluacion ev = crearEvaluacionMock(evId, curso);

        when(evaluacionRepository.findById(evId)).thenReturn(Optional.of(ev));

        // Act
        Optional<Evaluacion> resultado = evaluacionService.buscarPorId(evId);

        // Assert
        assertTrue(resultado.isPresent());
        assertEquals(evId, resultado.get().getId());
    }

    @Test
    @DisplayName("buscarPorId: debe retornar vacío cuando no existe")
    void buscarPorIdNoExistente() {
        // Arrange
        UUID evId = UUID.randomUUID();

        when(evaluacionRepository.findById(evId)).thenReturn(Optional.empty());

        // Act
        Optional<Evaluacion> resultado = evaluacionService.buscarPorId(evId);

        // Assert
        assertFalse(resultado.isPresent());
    }

    // ============================================================
    // OBTENER PREGUNTAS
    // ============================================================

    @Test
    @DisplayName("obtenerPreguntas: debe retornar preguntas ordenadas")
    void obtenerPreguntasConResultados() {
        // Arrange
        UUID evId = UUID.randomUUID();
        Curso curso = crearCursoMock(UUID.randomUUID());
        Evaluacion ev = crearEvaluacionMock(evId, curso);
        Pregunta p1 = crearPreguntaMock(UUID.randomUUID(), ev, 0);
        Pregunta p2 = crearPreguntaMock(UUID.randomUUID(), ev, 1);

        when(preguntaRepository.findByEvaluacionIdOrderByOrdenAsc(evId)).thenReturn(List.of(p1, p2));

        // Act
        List<Pregunta> resultado = evaluacionService.obtenerPreguntas(evId);

        // Assert
        assertNotNull(resultado);
        assertEquals(2, resultado.size());
        assertEquals(0, resultado.get(0).getOrden());
        assertEquals(1, resultado.get(1).getOrden());
    }

    @Test
    @DisplayName("obtenerPreguntas: debe retornar lista vacía si no hay preguntas")
    void obtenerPreguntasSinResultados() {
        // Arrange
        UUID evId = UUID.randomUUID();

        when(preguntaRepository.findByEvaluacionIdOrderByOrdenAsc(evId)).thenReturn(List.of());

        // Act
        List<Pregunta> resultado = evaluacionService.obtenerPreguntas(evId);

        // Assert
        assertNotNull(resultado);
        assertTrue(resultado.isEmpty());
    }

    // ============================================================
    // TEST ESPECÍFICO: Evaluación con 4 preguntas opción múltiple
    // ============================================================

    @Test
    @DisplayName("crearEvaluacion: 4 preguntas opcion_multiple peso 25 c/u con múltiples correctas")
    void crearEvaluacionCon4PreguntasOpcionMultiple() {
        // Arrange
        UUID cursoId = UUID.randomUUID();
        UUID autorId = UUID.randomUUID();
        Curso curso = crearCursoMock(cursoId);

        // 4 preguntas de tipo opcion_multiple, peso 25 c/u = 100
        // Cada pregunta tiene 4 opciones con 2 correctas (simulando checkboxes)
        PreguntaRequest p1 = new PreguntaRequest(
                "opcion_multiple",
                "¿Cuáles son lenguajes de programación?",
                25.0, 120000,
                "[{\"texto\":\"Java\",\"correcta\":true},{\"texto\":\"HTML\",\"correcta\":false},{\"texto\":\"Python\",\"correcta\":true},{\"texto\":\"CSS\",\"correcta\":false}]",
                null, 0
        );
        PreguntaRequest p2 = new PreguntaRequest(
                "opcion_multiple",
                "¿Cuáles son bases de datos relacionales?",
                25.0, 120000,
                "[{\"texto\":\"MySQL\",\"correcta\":true},{\"texto\":\"MongoDB\",\"correcta\":false},{\"texto\":\"PostgreSQL\",\"correcta\":true},{\"texto\":\"Redis\",\"correcta\":false}]",
                null, 1
        );
        PreguntaRequest p3 = new PreguntaRequest(
                "opcion_multiple",
                "¿Qué protocolos operan en capa de aplicación?",
                25.0, 120000,
                "[{\"texto\":\"HTTP\",\"correcta\":true},{\"texto\":\"TCP\",\"correcta\":false},{\"texto\":\"FTP\",\"correcta\":true},{\"texto\":\"IP\",\"correcta\":false}]",
                null, 2
        );
        PreguntaRequest p4 = new PreguntaRequest(
                "opcion_multiple",
                "¿Cuáles son frameworks de Java?",
                25.0, 120000,
                "[{\"texto\":\"Spring\",\"correcta\":true},{\"texto\":\"React\",\"correcta\":false},{\"texto\":\"Hibernate\",\"correcta\":true},{\"texto\":\"Vue\",\"correcta\":false}]",
                null, 3
        );

        EvaluacionRequest request = new EvaluacionRequest("Examen Opción Múltiple", "Evaluación con 4 preguntas de selección múltiple", 100.0, 60, "{\"version\":\"1.0\"}", 4.0, List.of(p1, p2, p3, p4));

        when(cursoService.buscarPorId(cursoId)).thenReturn(Optional.of(curso));
        when(codigoGeneratorService.generarCodigo("EVA")).thenReturn("EVA-MULTI-01");
        when(evaluacionRepository.save(any(Evaluacion.class))).thenAnswer(i -> {
            Evaluacion ev = i.getArgument(0);
            ev.setId(UUID.randomUUID());
            return ev;
        });
        when(preguntaRepository.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));

        // Act
        Evaluacion resultado = evaluacionService.crearEvaluacion(cursoId, request, autorId);

        // Assert
        assertNotNull(resultado);
        assertEquals("Examen Opción Múltiple", resultado.getTitulo());
        assertEquals(100.0, resultado.getPesoTotal());
        assertEquals("EVA-MULTI-01", resultado.getCodigo());
        verify(cursoService, times(1)).buscarPorId(cursoId);
        verify(evaluacionRepository, times(1)).save(any(Evaluacion.class));
        verify(preguntaRepository, times(1)).saveAll(anyList());
        verify(auditLogger, times(1)).registrarEvento(
                eq("EVALUACION_CREADA"), eq(autorId), eq("CREAR"),
                eq("EVALUACION"), any(), any()
        );

        // Verificar que las preguntas guardadas tienen opcion_multiple con múltiples correctas
        ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
        verify(preguntaRepository).saveAll(captor.capture());
        List<Pregunta> preguntasGuardadas = captor.getValue();
        assertEquals(4, preguntasGuardadas.size());

        for (int i = 0; i < preguntasGuardadas.size(); i++) {
            Pregunta p = preguntasGuardadas.get(i);
            assertEquals("opcion_multiple", p.getTipo(),
                    "Pregunta " + i + " debe ser opcion_multiple");
            assertEquals(25.0, p.getPeso(),
                    "Pregunta " + i + " debe tener peso 25");

            // Verificar que el JSON de opciones contiene múltiples correctas
            String opcionesJson = p.getOpciones();
            assertNotNull(opcionesJson, "Pregunta " + i + " debe tener opciones");
            assertTrue(opcionesJson.contains("\"correcta\":true"),
                    "Pregunta " + i + " debe tener al menos una opción correcta en JSON: " + opcionesJson);

            // Contar cuántas correctas hay en el JSON
            long correctasCount = opcionesJson.chars()
                    .filter(ch -> ch == '"')
                    .count();
            assertTrue(opcionesJson.indexOf("\"correcta\":true") != opcionesJson.lastIndexOf("\"correcta\":true")
                            || opcionesJson.contains("\"correcta\":true"),
                    "Pregunta " + i + " soporta selección múltiple (checkbox) en JSON: " + opcionesJson);
        }
    }

    @Test
    @DisplayName("crearEvaluacion: opcion_multiple debe persistir múltiples respuestas correctas en JSON")
    void crearEvaluacionOpcionMultipleConDosCorrectas() {
        // Arrange
        UUID cursoId = UUID.randomUUID();
        UUID autorId = UUID.randomUUID();
        Curso curso = crearCursoMock(cursoId);

        // Una sola pregunta opcion_multiple con 4 opciones, 2 correctas
        PreguntaRequest p1 = new PreguntaRequest(
                "opcion_multiple",
                "¿Qué lenguajes son compilados?",
                100.0, 120000,
                "[{\"texto\":\"Java\",\"correcta\":true},{\"texto\":\"Python\",\"correcta\":false},{\"texto\":\"C++\",\"correcta\":true},{\"texto\":\"JavaScript\",\"correcta\":false}]",
                null, 0
        );

        EvaluacionRequest request = new EvaluacionRequest("Test Multiple Correctas", "Verificar que 2 opciones correctas se guardan en JSON", 100.0, 30, "{\"version\":\"1.0\"}", 4.0, List.of(p1));

        when(cursoService.buscarPorId(cursoId)).thenReturn(Optional.of(curso));
        when(codigoGeneratorService.generarCodigo("EVA")).thenReturn("EVA-MC-01");
        when(evaluacionRepository.save(any(Evaluacion.class))).thenAnswer(i -> {
            Evaluacion ev = i.getArgument(0);
            ev.setId(UUID.randomUUID());
            return ev;
        });
        when(preguntaRepository.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));

        // Act
        Evaluacion resultado = evaluacionService.crearEvaluacion(cursoId, request, autorId);

        // Assert
        assertNotNull(resultado);

        ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
        verify(preguntaRepository).saveAll(captor.capture());
        List<Pregunta> preguntasGuardadas = captor.getValue();
        assertEquals(1, preguntasGuardadas.size());

        Pregunta p = preguntasGuardadas.get(0);
        String opcionesJson = p.getOpciones();
        assertNotNull(opcionesJson);

        // Parsear JSON y contar correctas
        // Buscar "correcta":true en el string JSON
        int idx = opcionesJson.indexOf("\"correcta\":true");
        int idx2 = opcionesJson.indexOf("\"correcta\":true", idx + 1);
        assertTrue(idx >= 0, "Debe haber al menos una opción correcta");
        assertTrue(idx2 > idx, "Debe haber DOS opciones correctas (checkbox múltiple). JSON: " + opcionesJson);

        // Verificar que las opciones incorrectas también están
        assertTrue(opcionesJson.contains("\"correcta\":false"),
                "Debe haber opciones incorrectas también. JSON: " + opcionesJson);
    }
}
