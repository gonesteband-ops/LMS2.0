package com.lms.controller;

import com.lms.model.Documento;
import com.lms.service.DocumentoService;
import com.lms.service.FirmaService;
import com.lms.util.SecurityUtils;
import io.minio.GetObjectResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Controlador REST de documentos (subida/descarga via MinIO).
 */
@RestController
@RequestMapping("/api/v1/documentos")
public class DocumentoController {

    private final DocumentoService documentoService;
    private final FirmaService firmaService;
    private final SecurityUtils securityUtils;

    public DocumentoController(DocumentoService documentoService, FirmaService firmaService, SecurityUtils securityUtils) {
        this.documentoService = documentoService;
        this.firmaService = firmaService;
        this.securityUtils = securityUtils;
    }

    @PostMapping("/upload")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'INSTRUCTOR')")
    public ResponseEntity<Documento> subir(@RequestParam("file") MultipartFile file,
                                           Authentication authentication) {
        UUID usuarioId = securityUtils.getCurrentUserId(authentication);
        return ResponseEntity.ok(documentoService.subirDocumento(file, usuarioId));
    }

    @PostMapping("/upload-curso")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'INSTRUCTOR')")
    public ResponseEntity<Documento> subirParaCurso(@RequestParam("file") MultipartFile file,
                                                     @RequestParam("cursoId") UUID cursoId,
                                                     Authentication authentication) {
        UUID usuarioId = securityUtils.getCurrentUserId(authentication);
        return ResponseEntity.ok(documentoService.subirDocumentoParaCurso(file, cursoId, usuarioId));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Documento>> listar(Authentication authentication) {
        UUID usuarioId = securityUtils.getCurrentUserId(authentication);
        boolean esStaff = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_SUPERADMIN")
                            || a.getAuthority().equals("ROLE_INSTRUCTOR")
                            || a.getAuthority().equals("ROLE_SUPERADMIN"));
        return ResponseEntity.ok(documentoService.listarDocumentosPorUsuario(usuarioId, esStaff));
    }

    /**
     * Lista los documentos vinculados a un curso específico.
     */
    @GetMapping("/curso/{cursoId}")
    public ResponseEntity<List<Documento>> listarPorCurso(@PathVariable UUID cursoId) {
        return ResponseEntity.ok(documentoService.listarDocumentosPorCurso(cursoId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Documento> obtener(@PathVariable UUID id) {
        return documentoService.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Endpoint de streaming con soporte para HTTP Range requests (progressive download).
     * Permite la reproducción progresiva de video directamente desde MinIO.
     */
    @GetMapping("/{id}/stream")
    public void stream(@PathVariable UUID id,
                       HttpServletRequest request,
                       HttpServletResponse response) {
        try {
            String rangeHeader = request.getHeader("Range");
            long fileSize = documentoService.obtenerTamanoTotal(id);
            String mimeType = documentoService.buscarPorId(id)
                    .map(Documento::getMimeType)
                    .orElse("application/octet-stream");

            if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
                // Parsear el rango: "bytes=start-end"
                String range = rangeHeader.substring("bytes=".length());
                String[] parts = range.split("-");
                long start = Long.parseLong(parts[0]);
                long end = parts.length > 1 && !parts[1].isEmpty()
                        ? Long.parseLong(parts[1])
                        : fileSize - 1;

                // Validar rango
                if (start >= fileSize) {
                    response.setStatus(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE.value());
                    response.setHeader("Content-Range", "bytes */" + fileSize);
                    return;
                }

                long contentLength = end - start + 1;

                response.setStatus(HttpStatus.PARTIAL_CONTENT.value());
                response.setHeader(HttpHeaders.CONTENT_TYPE, mimeType);
                response.setHeader(HttpHeaders.CONTENT_LENGTH, String.valueOf(contentLength));
                response.setHeader(HttpHeaders.ACCEPT_RANGES, "bytes");
                response.setHeader("Content-Range", "bytes " + start + "-" + end + "/" + fileSize);

                try (GetObjectResponse minioStream = documentoService.obtenerStreamParcial(id, start, contentLength);
                     java.io.OutputStream os = response.getOutputStream()) {
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    while ((bytesRead = minioStream.read(buffer)) != -1) {
                        os.write(buffer, 0, bytesRead);
                    }
                    os.flush();
                }
            } else {
                // Sin Range header: servir archivo completo
                response.setStatus(HttpStatus.OK.value());
                response.setHeader(HttpHeaders.CONTENT_TYPE, mimeType);
                response.setHeader(HttpHeaders.CONTENT_LENGTH, String.valueOf(fileSize));
                response.setHeader(HttpHeaders.ACCEPT_RANGES, "bytes");

                try (GetObjectResponse minioStream = documentoService.obtenerStream(id);
                     java.io.OutputStream os = response.getOutputStream()) {
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    while ((bytesRead = minioStream.read(buffer)) != -1) {
                        os.write(buffer, 0, bytesRead);
                    }
                    os.flush();
                }
            }
        } catch (IllegalArgumentException e) {
            response.setStatus(HttpStatus.NOT_FOUND.value());
        } catch (Exception e) {
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPERADMIN')")
    public ResponseEntity<Void> eliminar(@PathVariable UUID id, Authentication authentication) {
        UUID eliminadoPor = securityUtils.getCurrentUserId(authentication);
        documentoService.eliminarDocumento(id, eliminadoPor);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/toggle-habilitado")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'INSTRUCTOR')")
    public ResponseEntity<Map<String, Object>> toggleHabilitado(@PathVariable UUID id) {
        com.lms.model.Documento doc = documentoService.toggleHabilitado(id);
        return ResponseEntity.ok(Map.of(
                "id", doc.getId(),
                "habilitado", doc.isHabilitado()
        ));
    }

    @GetMapping("/{id}/hash")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, String>> obtenerHash(@PathVariable UUID id) {
        Documento doc = documentoService.buscarPorId(id)
                .orElseThrow(() -> new IllegalArgumentException("Documento no encontrado"));
        return ResponseEntity.ok(Map.of(
            "hashDocumento", doc.getHashSha256(),
            "codigo", doc.getCodigo()
        ));
    }


    @GetMapping("/{id}/firmado")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Boolean>> verificarFirma(
            @PathVariable UUID id, Authentication auth) {
        UUID usuarioId = securityUtils.getCurrentUserId(auth);
        boolean firmado = firmaService.verificarFirma(usuarioId, id);
        return ResponseEntity.ok(Map.of("firmado", firmado));
    }

}