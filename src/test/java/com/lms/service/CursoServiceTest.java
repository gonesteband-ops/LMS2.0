package com.lms.service;

import com.lms.dto.CursoRequest;
import com.lms.model.Curso;
import com.lms.model.VersionCurso;
import com.lms.repository.CursoRepository;
import com.lms.repository.VersionCursoRepository;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CursoService - Course Management & Versioning")
class CursoServiceTest {

    @Mock
    private CursoRepository cursoRepository;

    @Mock
    private VersionCursoRepository versionCursoRepository;

    @Mock
    private AuditLogger auditLogger;

    @InjectMocks
    private CursoService cursoService;

    @Test
    @DisplayName("crearCurso: debe crear curso con versión inicial v1")
    void crearCursoConVersionInicial() {
        // Arrange
        UUID autorId = UUID.randomUUID();
        CursoRequest request = new CursoRequest(
                "Introducción a Java", "Curso básico de Java", "PROGRAMACION",
                "Contenido del curso", null, null, false, null, null
        );

        Curso cursoGuardado = Curso.builder()
                .id(UUID.randomUUID())
                .titulo("Introducción a Java")
                .descripcion("Curso básico de Java")
                .versionActual(1)
                .estado("borrador")
                .categoria("PROGRAMACION")
                .hashSha256(HashUtils.sha256("Introducción a Java|Curso básico de Java"))
                .build();

        when(cursoRepository.save(any(Curso.class))).thenReturn(cursoGuardado);
        when(versionCursoRepository.save(any(VersionCurso.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        Curso resultado = cursoService.crearCurso(request, autorId);

        // Assert
        assertNotNull(resultado);
        assertEquals("Introducción a Java", resultado.getTitulo());
        assertEquals(1, resultado.getVersionActual());
        verify(cursoRepository, times(1)).save(any(Curso.class));
        verify(versionCursoRepository, times(1)).save(any(VersionCurso.class));
    }

    @Test
    @DisplayName("actualizarCurso: debe actualizar curso creando nueva versión")
    void actualizarCursoCreaNuevaVersion() {
        // Arrange
        UUID cursoId = UUID.randomUUID();
        UUID autorId = UUID.randomUUID();

        Curso cursoExistente = Curso.builder()
                .id(cursoId)
                .titulo("Curso Original")
                .descripcion("Descripción original")
                .versionActual(1)
                .estado("borrador")
                .categoria("PROGRAMACION")
                .build();

        VersionCurso versionAnterior = VersionCurso.builder()
                .id(UUID.randomUUID())
                .curso(cursoExistente)
                .numeroVersion(1)
                .hashSha256("hash-anterior")
                .build();

        when(cursoRepository.findById(cursoId)).thenReturn(Optional.of(cursoExistente));
        when(versionCursoRepository.findTopByCursoIdOrderByNumeroVersionDesc(cursoId))
                .thenReturn(Optional.of(versionAnterior));
        when(cursoRepository.save(any(Curso.class))).thenAnswer(i -> i.getArgument(0));

        CursoRequest request = new CursoRequest(
                "Curso Actualizado", "Descripción actualizada", "PROGRAMACION",
                "Nuevo contenido", "Agregar módulo avanzado", null, false, null, null
        );

        // Act
        Curso resultado = cursoService.actualizarCurso(cursoId, request, autorId);

        // Assert
        assertNotNull(resultado);
        assertEquals("Curso Actualizado", resultado.getTitulo());
        assertEquals(2, resultado.getVersionActual());
        verify(versionCursoRepository, times(1)).save(any(VersionCurso.class));
    }

    @Test
    @DisplayName("eliminarCurso: debe hacer soft-delete del curso")
    void eliminarCursoSoftDelete() {
        // Arrange
        UUID cursoId = UUID.randomUUID();
        UUID eliminadoPor = UUID.randomUUID();

        Curso curso = Curso.builder()
                .id(cursoId)
                .titulo("Curso a eliminar")
                .build();

        when(cursoRepository.findById(cursoId)).thenReturn(Optional.of(curso));
        when(cursoRepository.save(any(Curso.class))).thenReturn(curso);

        // Act
        cursoService.eliminarCurso(cursoId, eliminadoPor, "Curso descontinuado");

        // Assert
        assertNotNull(curso.getDeletedAt());
        assertEquals(eliminadoPor, curso.getDeletedBy());
        assertEquals("Curso descontinuado", curso.getReason());
        verify(cursoRepository, times(1)).save(curso);
    }

    @Test
    @DisplayName("listarCursos: debe retornar cursos activos paginados")
    void listarCursosActivos() {
        // Arrange
        PageRequest pageable = PageRequest.of(0, 10);
        Curso curso = Curso.builder()
                .id(UUID.randomUUID())
                .titulo("Curso Activo")
                .build();

        when(cursoRepository.findByDeletedAtIsNull(pageable))
                .thenReturn(new PageImpl<>(List.of(curso)));

        // Act
        Page<Curso> resultado = cursoService.listarCursos(pageable);

        // Assert
        assertNotNull(resultado);
        assertEquals(1, resultado.getContent().size());
        assertEquals("Curso Activo", resultado.getContent().get(0).getTitulo());
    }
}
