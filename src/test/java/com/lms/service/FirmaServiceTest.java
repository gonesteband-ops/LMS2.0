package com.lms.service;

import com.lms.dto.FirmaRequest;
import com.lms.dto.FirmaResponse;
import com.lms.model.Documento;
import com.lms.model.FirmaElectronica;
import com.lms.model.Usuario;
import com.lms.repository.DocumentoRepository;
import com.lms.repository.FirmaElectronicaRepository;
import com.lms.util.AuditLogger;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FirmaService - Electronic Signature (Ley 19.799)")
class FirmaServiceTest {

    @Mock
    private FirmaElectronicaRepository firmaRepository;

    @Mock
    private DocumentoRepository documentoRepository;

    @Mock
    private UsuarioService usuarioService;

    @Mock
    private AuditLogger auditLogger;

    @Mock
    private HttpServletRequest httpRequest;

    @InjectMocks
    private FirmaService firmaService;

    private String computeExpectedHash(String contenido, String hashAnterior) {
        return com.lms.util.HashUtils.sha256Encadenado(contenido, hashAnterior);
    }

    @Test
    @DisplayName("firmarDocumento: debe firmar con hash encadenado correctamente")
    void firmarDocumentoConHashEncadenado() {
        // Arrange
        UUID usuarioId = UUID.randomUUID();
        UUID documentoId = UUID.randomUUID();

        Usuario usuario = Usuario.builder()
                .id(usuarioId)
                .email("user@test.com")
                .consentimientoOtorgado(true)
                .build();

        Documento documento = Documento.builder()
                .id(documentoId)
                .nombreOriginal("contrato.pdf")
                .hashSha256("hash-documento-original")
                .mimeType("application/pdf")
                .tamanoBytes(1024)
                .build();

        FirmaRequest request = new FirmaRequest(documentoId, "Windows 10", "CL-SCL");

        String hashAnteriorExistente = "hash-firma-anterior";

        when(usuarioService.buscarPorId(usuarioId)).thenReturn(Optional.of(usuario));
        when(documentoRepository.findById(documentoId)).thenReturn(Optional.of(documento));
        when(firmaRepository.findUltimoHash()).thenReturn(Optional.of(hashAnteriorExistente));
        when(httpRequest.getHeader("X-Forwarded-For")).thenReturn("192.168.1.1");
        when(httpRequest.getHeader("User-Agent")).thenReturn("Mozilla/5.0");
        when(firmaRepository.save(any(FirmaElectronica.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        FirmaResponse response = firmaService.firmarDocumento(usuarioId, request, httpRequest);

        // Assert
        assertNotNull(response);
        assertEquals(usuarioId, response.usuarioId());
        assertEquals(documentoId, response.documentoId());
        assertEquals("hash-documento-original", response.hashDocumento());
        assertNotNull(response.hashSha256());
        assertEquals(hashAnteriorExistente, response.hashAnterior());
        // Verify hash is not empty and is valid SHA-256 (64 hex chars)
        assertEquals(64, response.hashSha256().length());
        verify(firmaRepository, times(1)).save(any(FirmaElectronica.class));
        verify(auditLogger, times(1)).registrarEvento(
                eq("FIRMA_REGISTRADA"), eq(usuarioId), eq("FIRMAR"),
                eq("DOCUMENTO"), eq(documentoId), any()
        );
    }

    @Test
    @DisplayName("firmarDocumento: debe lanzar excepción si el usuario no ha otorgado consentimiento")
    void firmarDocumentoSinConsentimiento() {
        // Arrange
        UUID usuarioId = UUID.randomUUID();
        UUID documentoId = UUID.randomUUID();

        Usuario usuario = Usuario.builder()
                .id(usuarioId)
                .email("user@test.com")
                .consentimientoOtorgado(false)
                .build();

        FirmaRequest request = new FirmaRequest(documentoId, "Windows 10", null);

        when(usuarioService.buscarPorId(usuarioId)).thenReturn(Optional.of(usuario));

        // Act & Assert
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> firmaService.firmarDocumento(usuarioId, request, httpRequest)
        );

        assertEquals("Debe otorgar consentimiento antes de firmar documentos", exception.getMessage());
        verify(firmaRepository, never()).save(any());
    }

    @Test
    @DisplayName("firmarDocumento: debe lanzar excepción si el usuario no existe")
    void firmarDocumentoUsuarioNoEncontrado() {
        // Arrange
        UUID usuarioId = UUID.randomUUID();
        FirmaRequest request = new FirmaRequest(UUID.randomUUID(), "Linux", null);

        when(usuarioService.buscarPorId(usuarioId)).thenReturn(Optional.empty());

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> firmaService.firmarDocumento(usuarioId, request, httpRequest)
        );

        assertEquals("Usuario no encontrado", exception.getMessage());
        verify(documentoRepository, never()).findById(any());
    }

    @Test
    @DisplayName("firmarDocumento: primera firma debe tener hashAnterior nulo (Génesis)")
    void primeraFirmaHashGenesis() {
        // Arrange
        UUID usuarioId = UUID.randomUUID();
        UUID documentoId = UUID.randomUUID();

        Usuario usuario = Usuario.builder()
                .id(usuarioId)
                .email("user@test.com")
                .consentimientoOtorgado(true)
                .build();

        Documento documento = Documento.builder()
                .id(documentoId)
                .nombreOriginal("contrato.pdf")
                .hashSha256("hash-documento")
                .mimeType("application/pdf")
                .tamanoBytes(2048)
                .build();

        FirmaRequest request = new FirmaRequest(documentoId, "macOS", null);

        when(usuarioService.buscarPorId(usuarioId)).thenReturn(Optional.of(usuario));
        when(documentoRepository.findById(documentoId)).thenReturn(Optional.of(documento));
        when(firmaRepository.findUltimoHash()).thenReturn(Optional.empty());
        when(httpRequest.getHeader("X-Forwarded-For")).thenReturn(null);
        when(httpRequest.getHeader("X-Real-IP")).thenReturn("10.0.0.1");
        when(httpRequest.getHeader("User-Agent")).thenReturn("curl/7.68");
        when(firmaRepository.save(any(FirmaElectronica.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        FirmaResponse response = firmaService.firmarDocumento(usuarioId, request, httpRequest);

        // Assert
        assertNotNull(response);
        assertNull(response.hashAnterior(), "La primera firma debe tener hashAnterior nulo (Génesis)");
        verify(firmaRepository, times(1)).save(any(FirmaElectronica.class));
    }
}
