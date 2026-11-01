package com.lms.service;

import com.lms.model.Curso;
import com.lms.model.PaqueteScorm;
import com.lms.model.ScormDatoAlumno;
import com.lms.repository.CursoRepository;
import com.lms.repository.PaqueteScormRepository;
import com.lms.repository.ScormDatoAlumnoRepository;
import com.lms.util.AuditLogger;
import com.lms.util.HashUtils;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Servicio de gestión de paquetes SCORM.
 * <p>
 * Maneja subida, validación, extracción a MinIO y registro en BD.
 */
@Service
@Transactional
public class ScormService {

    private final PaqueteScormRepository paqueteScormRepository;
    private final ScormDatoAlumnoRepository scormDatoAlumnoRepository;
    private final MinioClient minioClient;
    private final AuditLogger auditLogger;
    private final CodigoGeneratorService codigoGeneratorService;
    private final CursoRepository cursoRepository;

    @Value("${app.minio.bucket-name}")
    private String bucketName;

    public ScormService(PaqueteScormRepository paqueteScormRepository,
                        ScormDatoAlumnoRepository scormDatoAlumnoRepository,
                        MinioClient minioClient,
                        AuditLogger auditLogger,
                        CodigoGeneratorService codigoGeneratorService,
                        CursoRepository cursoRepository) {
        this.paqueteScormRepository = paqueteScormRepository;
        this.scormDatoAlumnoRepository = scormDatoAlumnoRepository;
        this.minioClient = minioClient;
        this.auditLogger = auditLogger;
        this.codigoGeneratorService = codigoGeneratorService;
        this.cursoRepository = cursoRepository;
    }

    /**
     * Sube un paquete SCORM (ZIP), lo valida, extrae a MinIO y registra en BD.
     *
     * @param zip       Archivo ZIP del paquete SCORM
     * @param cursoId   ID del curso al que pertenece
     * @param usuarioId ID del usuario que sube el paquete
     * @return PaqueteScorm registrado
     */
    public PaqueteScorm subirPaqueteScorm(MultipartFile zip, UUID cursoId, UUID usuarioId) {
        Curso curso = cursoRepository.findByIdAndDeletedAtIsNull(cursoId)
                .orElseThrow(() -> new IllegalArgumentException("Curso no encontrado: " + cursoId));
        try {
            byte[] zipBytes = zip.getBytes();
            String hashSha256 = HashUtils.sha256(zipBytes);
            String paquetePrefix = "scorm/" + UUID.randomUUID();

            // 1. Validar ZIP y extraer archivos en memoria
            Map<String, byte[]> extractedFiles = new LinkedHashMap<>();
            boolean hasManifest = false;
            byte[] manifestBytes = null;

            try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
                ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    if (entry.isDirectory()) continue;

                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    byte[] buffer = new byte[8192];
                    int len;
                    while ((len = zis.read(buffer)) != -1) {
                        baos.write(buffer, 0, len);
                    }
                    byte[] fileBytes = baos.toByteArray();
                    extractedFiles.put(entry.getName(), fileBytes);

                    if ("imsmanifest.xml".equalsIgnoreCase(entry.getName())) {
                        hasManifest = true;
                        manifestBytes = fileBytes;
                    }
                }
            }

            if (!hasManifest || manifestBytes == null) {
                throw new IllegalArgumentException("El ZIP no contiene un archivo imsmanifest.xml válido");
            }

            // 2. Parsear imsmanifest.xml
            String titulo = "";
            String versionScorm = "1.2";
            String indexFile = "index.html";

            try {
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
                factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
                factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
                DocumentBuilder builder = factory.newDocumentBuilder();
                org.w3c.dom.Document doc = builder.parse(new ByteArrayInputStream(manifestBytes));

                // Título desde <organization> / <title>
                org.w3c.dom.NodeList orgList = doc.getElementsByTagName("organization");
                if (orgList.getLength() > 0) {
                    org.w3c.dom.Element orgElem = (org.w3c.dom.Element) orgList.item(0);
                    org.w3c.dom.NodeList titleList = orgElem.getElementsByTagName("title");
                    if (titleList.getLength() > 0) {
                        titulo = titleList.item(0).getTextContent().trim();
                    }
                }

                // Versión SCORM
                org.w3c.dom.NodeList schemaList = doc.getElementsByTagName("schemaversion");
                if (schemaList.getLength() > 0) {
                    String schemaVer = schemaList.item(0).getTextContent().trim();
                    if (schemaVer.startsWith("2004")) {
                        versionScorm = "2004";
                    }
                }

                // Buscar recurso con index.html / index.htm
                org.w3c.dom.NodeList resourceList = doc.getElementsByTagName("resource");
                for (int i = 0; i < resourceList.getLength(); i++) {
                    org.w3c.dom.Element resource = (org.w3c.dom.Element) resourceList.item(i);
                    String href = resource.getAttribute("href");
                    if (href != null && !href.isEmpty()) {
                        // El primer recurso con href suele ser el index
                        if (href.endsWith(".html") || href.endsWith(".htm")) {
                            indexFile = href;
                            break;
                        }
                        if (i == 0) {
                            indexFile = href;
                        }
                    }
                }
            } catch (Exception e) {
                // Si falla el parseo, usar valores por defecto
                titulo = zip.getOriginalFilename() != null
                        ? zip.getOriginalFilename().replaceAll("(?i)\\.zip$", "")
                        : "Paquete SCORM";
            }

            if (titulo.isEmpty()) {
                titulo = zip.getOriginalFilename() != null
                        ? zip.getOriginalFilename().replaceAll("(?i)\\.zip$", "")
                        : "Paquete SCORM";
            }

            // 3. Subir archivos extraídos a MinIO
            long tamanoTotal = 0;
            for (Map.Entry<String, byte[]> entry : extractedFiles.entrySet()) {
                String objectName = paquetePrefix + "/" + entry.getKey();
                byte[] content = entry.getValue();
                tamanoTotal += content.length;

                minioClient.putObject(
                        PutObjectArgs.builder()
                                .bucket(bucketName)
                                .object(objectName)
                                .stream(new ByteArrayInputStream(content), content.length, -1)
                                .contentType(obtenerContentType(entry.getKey()))
                                .build()
                );
            }

            // 4. Subir ZIP original a MinIO
            String zipObjectName = paquetePrefix + "/" + (zip.getOriginalFilename() != null ? zip.getOriginalFilename() : "paquete.zip");
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(zipObjectName)
                            .stream(new ByteArrayInputStream(zipBytes), zipBytes.length, -1)
                            .contentType("application/zip")
                            .build()
            );

            String urlZip = "/api/v1/scorm/" + paquetePrefix + "/" + (zip.getOriginalFilename() != null ? zip.getOriginalFilename() : "paquete.zip");
            String urlIndex = "/api/v1/scorm/" + paquetePrefix + "/" + indexFile;

            // 5. Generar código alfanumérico correlativo
            String codigo = codigoGeneratorService.generarCodigo("SCO");

            // 6. Registrar en BD
            PaqueteScorm paquete = PaqueteScorm.builder()
                    .curso(curso)
                    .titulo(titulo)
                    .versionScorm(versionScorm)
                    .urlZip(urlZip)
                    .urlIndex(urlIndex)
                    .codigo(codigo)
                    .hashSha256(hashSha256)
                    .tamanoBytes(tamanoTotal)
                    .build();

            paquete = paqueteScormRepository.save(paquete);

            // 7. Auditoría
            auditLogger.registrarEvento(
                    "SCORM_SUBIDO",
                    usuarioId,
                    "SUBIR",
                    "PAQUETE_SCORM",
                    paquete.getId(),
                    Map.of(
                            "cursoId", cursoId.toString(),
                            "titulo", titulo,
                            "codigo", codigo,
                            "versionScorm", versionScorm,
                            "hash", hashSha256,
                            "tamanoBytes", tamanoTotal
                    )
            );

            return paquete;

        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Error al subir paquete SCORM", e);
        }
    }

    /**
     * Obtiene los datos del manifiesto de un paquete SCORM.
     */
    @Transactional(readOnly = true)
    public PaqueteScorm obtenerManifiesto(UUID paqueteId) {
        return paqueteScormRepository.findByIdAndDeletedAtIsNull(paqueteId)
                .orElseThrow(() -> new IllegalArgumentException("Paquete SCORM no encontrado: " + paqueteId));
    }

    /**
     * Registra un dato de progreso del alumno en un paquete SCORM.
     */
    public ScormDatoAlumno registrarDatoAlumno(UUID paqueteId, UUID usuarioId, UUID intentoId, String clave, String valor) {
        ScormDatoAlumno dato = ScormDatoAlumno.builder()
                .paqueteId(paqueteId)
                .usuarioId(usuarioId)
                .intentoId(intentoId)
                .clave(clave)
                .valor(valor)
                .build();

        return scormDatoAlumnoRepository.save(dato);
    }

    /**
     * Obtiene todos los datos de progreso de un alumno en un paquete SCORM.
     */
    @Transactional(readOnly = true)
    public List<ScormDatoAlumno> obtenerDatosAlumno(UUID paqueteId, UUID usuarioId) {
        return scormDatoAlumnoRepository.findByPaqueteIdAndUsuarioId(paqueteId, usuarioId);
    }

    /**
     * Obtiene un valor específico del alumno en un intento.
     */
    @Transactional(readOnly = true)
    public Optional<ScormDatoAlumno> obtenerDatoAlumno(UUID paqueteId, UUID usuarioId, UUID intentoId, String clave) {
        return scormDatoAlumnoRepository.findByPaqueteIdAndUsuarioIdAndIntentoIdAndClave(paqueteId, usuarioId, intentoId, clave);
    }

    /**
     * Busca un paquete SCORM por su código alfanumérico.
     *
     * @param codigo Código alfanumérico (ej: SCO0001)
     * @return Optional con el paquete SCORM
     */
    @Transactional(readOnly = true)
    public Optional<PaqueteScorm> buscarPorCodigo(String codigo) {
        return paqueteScormRepository.findByCodigoAndDeletedAtIsNull(codigo);
    }

    /**
     * Lista paquetes SCORM de un curso.
     */
    @Transactional(readOnly = true)
    public List<PaqueteScorm> listarPorCurso(UUID cursoId) {
        return paqueteScormRepository.findByCursoIdAndDeletedAtIsNullOrderByCreatedAtDesc(cursoId);
    }

    /**
     * Activa/desactiva un paquete SCORM (toggle habilitado).
     */
    public PaqueteScorm toggleHabilitado(UUID paqueteId) {
        PaqueteScorm paquete = paqueteScormRepository.findByIdAndDeletedAtIsNull(paqueteId)
                .orElseThrow(() -> new IllegalArgumentException("Paquete SCORM no encontrado: " + paqueteId));
        paquete.setHabilitado(!paquete.isHabilitado());
        return paqueteScormRepository.save(paquete);
    }

    /**
     * Elimina un paquete SCORM (soft delete).
     */
    public void eliminarPaquete(UUID paqueteId, UUID eliminadoPor) {
        paqueteScormRepository.findByIdAndDeletedAtIsNull(paqueteId).ifPresent(paquete -> {
            paquete.setDeletedAt(LocalDateTime.now());
            paquete.setDeletedBy(eliminadoPor);
            paqueteScormRepository.save(paquete);

            auditLogger.registrarEvento(
                    "SCORM_ELIMINADO",
                    eliminadoPor,
                    "ELIMINAR",
                    "PAQUETE_SCORM",
                    paqueteId,
                    Map.of("titulo", paquete.getTitulo(), "cursoId", paquete.getCurso().getId().toString(), "codigo", paquete.getCodigo())
            );
        });
    }

    private String obtenerContentType(String fileName) {
        String lower = fileName.toLowerCase();
        if (lower.endsWith(".html") || lower.endsWith(".htm")) return "text/html; charset=UTF-8";
        if (lower.endsWith(".css")) return "text/css; charset=UTF-8";
        if (lower.endsWith(".js")) return "application/javascript; charset=UTF-8";
        if (lower.endsWith(".xml")) return "application/xml; charset=UTF-8";
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".gif")) return "image/gif";
        if (lower.endsWith(".svg")) return "image/svg+xml";
        if (lower.endsWith(".swf")) return "application/x-shockwave-flash";
        if (lower.endsWith(".mp4")) return "video/mp4";
        if (lower.endsWith(".mp3")) return "audio/mpeg";
        if (lower.endsWith(".pdf")) return "application/pdf";
        return "application/octet-stream";
    }
}
