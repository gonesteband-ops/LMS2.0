package com.lms.controller;

import com.lms.model.PaqueteScorm;
import com.lms.model.ScormDatoAlumno;
import com.lms.service.CodigoGeneratorService;
import com.lms.repository.ScormDatoAlumnoRepository;
import com.lms.service.ScormRuntimeService;
import com.lms.service.ScormService;
import com.lms.util.SecurityUtils;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Controlador de paquetes SCORM.
 * <p>
 * Gestiona subida, listado, reproducción y runtime API de SCORM 1.2 / 2004.
 */
@RestController
@RequestMapping("/api/v1")
public class ScormController {

    private static final Logger log = LoggerFactory.getLogger(ScormController.class);

    private final ScormService scormService;
    private final ScormRuntimeService scormRuntimeService;
    private final SecurityUtils securityUtils;
    private final MinioClient minioClient;
    private final CodigoGeneratorService codigoGeneratorService;
    private final ScormDatoAlumnoRepository scormDatoAlumnoRepository;

    @Value("${app.minio.bucket-name}")
    private String bucketName;

    public ScormController(ScormService scormService,
                           ScormRuntimeService scormRuntimeService,
                           SecurityUtils securityUtils,
                           MinioClient minioClient,
                           CodigoGeneratorService codigoGeneratorService,
                           ScormDatoAlumnoRepository scormDatoAlumnoRepository) {
        this.scormService = scormService;
        this.scormRuntimeService = scormRuntimeService;
        this.securityUtils = securityUtils;
        this.minioClient = minioClient;
        this.codigoGeneratorService = codigoGeneratorService;
        this.scormDatoAlumnoRepository = scormDatoAlumnoRepository;
    }

    // ============================================================
    // Gestión de paquetes SCORM
    // ============================================================

    /**
     * Sube un paquete SCORM a un curso.
     */
    @PostMapping("/cursos/{cursoId}/scorm/upload")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'INSTRUCTOR')")
    public ResponseEntity<Map<String, Object>> subirPaqueteScorm(
            @PathVariable UUID cursoId,
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {

        UUID usuarioId = securityUtils.getCurrentUserId(authentication);
        PaqueteScorm paquete = scormService.subirPaqueteScorm(file, cursoId, usuarioId);

        return ResponseEntity.ok(Map.of(
                "result", "success",
                "id", paquete.getId().toString(),
                "titulo", paquete.getTitulo(),
                "codigo", paquete.getCodigo() != null ? paquete.getCodigo() : "",
                "versionScorm", paquete.getVersionScorm()
        ));
    }

    /**
     * Lista paquetes SCORM de un curso.
     */
    @GetMapping("/cursos/{cursoId}/scorm")
    public ResponseEntity<List<Map<String, Object>>> listarPaquetesScorm(@PathVariable UUID cursoId,
                                                                          Authentication authentication) {
        List<PaqueteScorm> paquetes = scormService.listarPorCurso(cursoId);

        UUID uid = null;
        if (authentication != null && authentication.isAuthenticated()) {
            try { uid = securityUtils.getCurrentUserId(authentication); } catch (Exception e) {}
        }
        final UUID authUid = uid;

        List<Map<String, Object>> response = paquetes.stream()
                .map(p -> {
                    java.util.Map<String, Object> map = new java.util.HashMap<>();
                    map.put("id", p.getId().toString());
                    map.put("titulo", p.getTitulo());
                    map.put("codigo", p.getCodigo() != null ? p.getCodigo() : "");
                    map.put("versionScorm", p.getVersionScorm());
                    map.put("tamanoBytes", p.getTamanoBytes());
                    map.put("createdAt", p.getCreatedAt() != null ? p.getCreatedAt().toString() : "");
                    map.put("habilitado", p.isHabilitado());
                    boolean completo = false;
                    if (authUid != null) {
                        try {
                            var datos = scormDatoAlumnoRepository.findByPaqueteIdAndUsuarioId(p.getId(), authUid);
                            completo = datos.stream().anyMatch(d ->
                                "completed".equals(d.getValor()) || "passed".equals(d.getValor()));
                        } catch (Exception ignored) {}
                    }
                    map.put("completadoPorAlumno", completo);
                    return map;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    /**
     * Busca un paquete SCORM por su código alfanumérico.
     */
    @GetMapping("/scorm/buscar")
    public ResponseEntity<Map<String, Object>> buscarPorCodigo(@RequestParam String codigo) {
        return scormService.buscarPorCodigo(codigo)
                .map(p -> ResponseEntity.ok(Map.<String, Object>of(
                        "id", p.getId().toString(),
                        "titulo", p.getTitulo(),
                        "codigo", p.getCodigo(),
                        "versionScorm", p.getVersionScorm(),
                        "tamanoBytes", p.getTamanoBytes(),
                        "urlZip", p.getUrlZip(),
                        "urlIndex", p.getUrlIndex(),
                        "cursoId", p.getCurso().getId().toString(),
                        "createdAt", p.getCreatedAt() != null ? p.getCreatedAt().toString() : ""
                )))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Elimina un paquete SCORM (soft delete).
     */
    @DeleteMapping("/scorm/{paqueteId}")
    @PreAuthorize("hasAnyRole('SUPERADMIN')")
    public ResponseEntity<Map<String, String>> eliminarPaqueteScorm(@PathVariable UUID paqueteId,
                                                                     Authentication authentication) {
        UUID usuarioId = securityUtils.getCurrentUserId(authentication);
        scormService.eliminarPaquete(paqueteId, usuarioId);

        return ResponseEntity.ok(Map.of("result", "success"));
    }

    @PatchMapping("/scorm/{paqueteId}/toggle-habilitado")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'INSTRUCTOR')")
    public ResponseEntity<Map<String, Object>> toggleHabilitado(@PathVariable UUID paqueteId) {
        PaqueteScorm paquete = scormService.toggleHabilitado(paqueteId);
        return ResponseEntity.ok(Map.of(
                "id", paquete.getId().toString(),
                "habilitado", paquete.isHabilitado()
        ));
    }

    /**
     * Carga la vista del reproductor SCORM.
     */
    @GetMapping("/scorm/{paqueteId}/play")
    public String reproducirPaqueteScorm(@PathVariable UUID paqueteId, Model model) {
        PaqueteScorm paquete = scormService.obtenerManifiesto(paqueteId);

        model.addAttribute("paqueteId", paquete.getId().toString());
        model.addAttribute("titulo", paquete.getTitulo());
        model.addAttribute("versionScorm", paquete.getVersionScorm());
        model.addAttribute("urlIndex", paquete.getUrlIndex());

        return "cursos/scorm-player";
    }

    /**
     * Sirve archivos de contenido SCORM desde MinIO.
     * Captura cualquier ruta bajo /api/v1/scorm/** que no coincida con endpoints específicos
     * (como /play, /initialize, /getValue, etc.) y sirve el archivo directamente desde MinIO.
     * <p>
     * La URL almacenada en urlIndex tiene el formato: /api/v1/scorm/scorm/<uuid>/<archivo>
     * El object path en MinIO es: scorm/<uuid>/<archivo>
     */
    @GetMapping("/scorm/**")
    public void servirArchivoScorm(HttpServletRequest request, HttpServletResponse response) {
        String requestURI = request.getRequestURI();
        String objectPath = requestURI.substring("/api/v1/scorm/".length());

        log.debug("Sirviendo archivo SCORM desde MinIO: {}", objectPath);

        try (InputStream stream = minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(bucketName)
                        .object(objectPath)
                        .build())) {

            String contentType = obtenerContentType(objectPath);
            response.setContentType(contentType);
            response.setHeader("Cache-Control", "private, max-age=3600");
            stream.transferTo(response.getOutputStream());
            response.flushBuffer();

            log.info("Archivo SCORM servido exitosamente: {} ({} bytes)", objectPath, response.getBufferSize());

        } catch (Exception e) {
            log.error("Error al servir archivo SCORM '{}' desde MinIO: {}", objectPath, e.getMessage());
            try {
                response.sendError(HttpServletResponse.SC_NOT_FOUND,
                        "Archivo SCORM no encontrado: " + objectPath);
            } catch (Exception ignored) {
                // Si ya se commiteó la respuesta, ignorar
            }
        }
    }

    private String obtenerContentType(String fileName) {
        String lower = fileName.toLowerCase();
        if (lower.endsWith(".html") || lower.endsWith(".htm")) return "text/html; charset=UTF-8";
        if (lower.endsWith(".css")) return "text/css; charset=UTF-8";
        if (lower.endsWith(".js")) return "application/javascript; charset=UTF-8";
        if (lower.endsWith(".xml")) return "application/xml; charset=UTF-8";
        if (lower.endsWith(".json")) return "application/json; charset=UTF-8";
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".gif")) return "image/gif";
        if (lower.endsWith(".svg")) return "image/svg+xml";
        if (lower.endsWith(".swf")) return "application/x-shockwave-flash";
        if (lower.endsWith(".mp4")) return "video/mp4";
        if (lower.endsWith(".mp3")) return "audio/mpeg";
        if (lower.endsWith(".pdf")) return "application/pdf";
        if (lower.endsWith(".woff2")) return "font/woff2";
        if (lower.endsWith(".woff")) return "font/woff";
        if (lower.endsWith(".ttf")) return "font/ttf";
        if (lower.endsWith(".eot")) return "application/vnd.ms-fontobject";
        return "application/octet-stream";
    }

    // ============================================================
    // API Runtime SCORM
    // ============================================================

    /**
     * Inicializa la comunicación con el paquete SCORM.
     */
    @PostMapping("/scorm/{paqueteId}/initialize")
    public ResponseEntity<Map<String, String>> initialize(@PathVariable UUID paqueteId,
                                                          Authentication authentication) {
        UUID usuarioId = securityUtils.getCurrentUserId(authentication);
        String result = scormRuntimeService.lmsInitialize(paqueteId, usuarioId);
        return ResponseEntity.ok(Map.of("result", result));
    }

    /**
     * Obtiene un valor del modelo de datos SCORM.
     */
    @GetMapping("/scorm/{paqueteId}/getValue")
    public ResponseEntity<Map<String, String>> getValue(
            @PathVariable UUID paqueteId,
            @RequestParam String clave,
            Authentication authentication) {

        UUID usuarioId = securityUtils.getCurrentUserId(authentication);
        String valor = scormRuntimeService.lmsGetValue(paqueteId, usuarioId, clave);
        return ResponseEntity.ok(Map.of("valor", valor));
    }

    /**
     * Establece un valor en el modelo de datos SCORM.
     */
    @PostMapping("/scorm/{paqueteId}/setValue")
    public ResponseEntity<Map<String, String>> setValue(
            @PathVariable UUID paqueteId,
            @RequestBody Map<String, String> body,
            Authentication authentication) {

        UUID usuarioId = securityUtils.getCurrentUserId(authentication);
        String clave = body.get("clave");
        String valor = body.get("valor");

        if (clave == null || valor == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "clave y valor son requeridos"));
        }

        String result = scormRuntimeService.lmsSetValue(paqueteId, usuarioId, clave, valor);
        return ResponseEntity.ok(Map.of("result", result));
    }

    /**
     * Hace commit de los valores SCORM.
     */
    @PostMapping("/scorm/{paqueteId}/commit")
    public ResponseEntity<Map<String, String>> commit(@PathVariable UUID paqueteId,
                                                      Authentication authentication) {
        UUID usuarioId = securityUtils.getCurrentUserId(authentication);
        String result = scormRuntimeService.lmsCommit(paqueteId, usuarioId);
        return ResponseEntity.ok(Map.of("result", result));
    }

    /**
     * Finaliza la comunicación con el paquete SCORM.
     */
    @PostMapping("/scorm/{paqueteId}/finish")
    public ResponseEntity<Map<String, String>> finish(@PathVariable UUID paqueteId,
                                                      Authentication authentication) {
        UUID usuarioId = securityUtils.getCurrentUserId(authentication);
        String result = scormRuntimeService.lmsFinish(paqueteId, usuarioId);
        return ResponseEntity.ok(Map.of("result", result));
    }

    /**
     * Obtiene el último error SCORM.
     */
    @GetMapping("/scorm/lastError")
    public ResponseEntity<Map<String, Object>> getLastError() {
        int errorCode = scormRuntimeService.lmsGetLastError();
        String errorString = scormRuntimeService.lmsGetErrorString(errorCode);
        String diagnostic = scormRuntimeService.lmsGetDiagnostic(errorCode);

        return ResponseEntity.ok(Map.of(
                "errorCode", errorCode,
                "errorString", errorString,
                "diagnostic", diagnostic
        ));
    }
}
