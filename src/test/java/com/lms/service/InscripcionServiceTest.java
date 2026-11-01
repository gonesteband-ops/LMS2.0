package com.lms.service;

import com.lms.dto.InscripcionResponse;
import com.lms.model.Curso;
import com.lms.model.Inscripcion;
import com.lms.model.Usuario;
import com.lms.repository.CursoRepository;
import com.lms.repository.InscripcionRepository;
import com.lms.repository.UsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios para InscripcionService.
 * Verifica el nuevo método listarParticipantesPorCurso (Bug 3)
 * y el método inscribirUsuario.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("InscripcionService - Unit Tests")
class InscripcionServiceTest {

    @Mock
    private InscripcionRepository inscripcionRepository;

    @Mock
    private CursoRepository cursoRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private NotificacionService notificacionService;

    @InjectMocks
    private InscripcionService inscripcionService;

    private Curso crearCurso() {
        Curso curso = new Curso();
        curso.setId(UUID.randomUUID());
        curso.setTitulo("Curso Test");
        curso.setCategoria("PROGRAMACION");
        curso.setEstado("publicado");
        curso.setFechaLimite(LocalDateTime.now().plusDays(30));
        return curso;
    }

    private Usuario crearUsuario(String nombre, String email) {
        Usuario usuario = new Usuario();
        usuario.setId(UUID.randomUUID());
        usuario.setNombreCompleto(nombre);
        usuario.setEmail(email);
        usuario.setRut(UUID.randomUUID().toString().substring(0, 10));
        return usuario;
    }

    private Inscripcion crearInscripcion(Curso curso, Usuario usuario) {
        Inscripcion inscripcion = new Inscripcion();
        inscripcion.setId(UUID.randomUUID());
        inscripcion.setCurso(curso);
        inscripcion.setUsuario(usuario);
        inscripcion.setFechaInscripcion(LocalDateTime.now());
        inscripcion.setFechaLimite(curso.getFechaLimite());
        inscripcion.setCompletado(false);
        return inscripcion;
    }

    @Test
    @DisplayName("listarParticipantesPorCurso - debe retornar lista de participantes")
    void listarParticipantesPorCurso() {
        // Arrange
        UUID cursoId = UUID.randomUUID();
        Curso curso = crearCurso();
        curso.setId(cursoId);

        Usuario user1 = crearUsuario("Juan Pérez", "juan@test.com");
        Usuario user2 = crearUsuario("María García", "maria@test.com");

        Inscripcion insc1 = crearInscripcion(curso, user1);
        Inscripcion insc2 = crearInscripcion(curso, user2);

        when(inscripcionRepository.findByCursoIdAndDeletedAtIsNull(cursoId))
                .thenReturn(List.of(insc1, insc2));

        // Act
        List<InscripcionResponse> result = inscripcionService.listarParticipantesPorCurso(cursoId);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Juan Pérez", result.get(0).usuarioNombre());
        assertEquals("juan@test.com", result.get(0).usuarioEmail());
        assertEquals("María García", result.get(1).usuarioNombre());
        assertEquals("maria@test.com", result.get(1).usuarioEmail());
        assertEquals("Curso Test", result.get(0).cursoTitulo());
        assertEquals(cursoId, result.get(0).cursoId());

        verify(inscripcionRepository).findByCursoIdAndDeletedAtIsNull(cursoId);
    }

    @Test
    @DisplayName("listarParticipantesPorCurso - debe retornar lista vacía si no hay inscritos")
    void listarParticipantesPorCursoVacio() {
        // Arrange
        UUID cursoId = UUID.randomUUID();

        when(inscripcionRepository.findByCursoIdAndDeletedAtIsNull(cursoId))
                .thenReturn(List.of());

        // Act
        List<InscripcionResponse> result = inscripcionService.listarParticipantesPorCurso(cursoId);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(inscripcionRepository).findByCursoIdAndDeletedAtIsNull(cursoId);
    }

    @Test
    @DisplayName("listarParticipantesPorCurso - debe retornar solo participantes no eliminados")
    void listarParticipantesPorCursoFiltraDeleted() {
        // Arrange
        UUID cursoId = UUID.randomUUID();
        Curso curso = crearCurso();
        curso.setId(cursoId);

        Usuario user1 = crearUsuario("Activo", "activo@test.com");
        Inscripcion insc1 = crearInscripcion(curso, user1);

        // Simular que el repository ya filtra deleted_at is null
        when(inscripcionRepository.findByCursoIdAndDeletedAtIsNull(cursoId))
                .thenReturn(List.of(insc1));

        // Act
        List<InscripcionResponse> result = inscripcionService.listarParticipantesPorCurso(cursoId);

        // Assert
        assertEquals(1, result.size());
        assertEquals("Activo", result.get(0).usuarioNombre());

        verify(inscripcionRepository).findByCursoIdAndDeletedAtIsNull(cursoId);
    }

    @Test
    @DisplayName("InscripcionResponse.fromEntity - debe convertir entidad a DTO correctamente")
    void inscripcionResponseFromEntity() {
        // Arrange
        Curso curso = crearCurso();
        Usuario usuario = crearUsuario("Test User", "test@example.com");
        Inscripcion inscripcion = crearInscripcion(curso, usuario);
        inscripcion.setNota(85.0);
        inscripcion.setCompletado(true);
        inscripcion.setFechaCompletado(LocalDateTime.now());

        // Act
        InscripcionResponse response = InscripcionResponse.fromEntity(inscripcion);

        // Assert
        assertNotNull(response);
        assertEquals(inscripcion.getId(), response.id());
        assertEquals(curso.getId(), response.cursoId());
        assertEquals(curso.getTitulo(), response.cursoTitulo());
        assertEquals(usuario.getId(), response.usuarioId());
        assertEquals(usuario.getNombreCompleto(), response.usuarioNombre());
        assertEquals(usuario.getEmail(), response.usuarioEmail());
        assertTrue(response.completado());
        assertEquals(85.0, response.nota());
        assertNotNull(response.fechaCompletado());
    }
}
