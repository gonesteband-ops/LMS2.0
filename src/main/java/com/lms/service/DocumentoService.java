package com.lms.service;

import com.lms.model.CursoDocumento;
import com.lms.model.Documento;
import com.lms.repository.CursoDocumentoRepository;
import com.lms.repository.DocumentoRepository;
import com.lms.util.AuditLogger;
import com.lms.util.HashUtils;
import io.minio.GetObjectArgs;
import io.minio.GetObjectResponse;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Servicio de gestión de documentos (almacenamiento en MinIO + metadatos en BD).
 * <p>
 * Los archivos físicos se almacenan en MinIO, la BD solo guarda metadatos y hash.
 */
@Service
@Transactional
public class DocumentoService {

    private final DocumentoRepository documentoRepository;
    private final CursoDocumentoRepository cursoDocumentoRepository;
    private final com.lms.repository.CursoRepository cursoRepository;
    private final MinioClient minioClient;
    private final AuditLogger auditLogger;
    private final CodigoGeneratorService codigoGeneratorService;

    @Value("${app.minio.bucket-name}")
    private String bucketName;

    public DocumentoService(DocumentoRepository documentoRepository,
                            CursoDocumentoRepository cursoDocumentoRepository,
                            com.lms.repository.CursoRepository cursoRepository,
                            MinioClient minioClient,
                            AuditLogger auditLogger,
                            CodigoGeneratorService codigoGeneratorService) {
        this.documentoRepository = documentoRepository;
        this.cursoDocumentoRepository = cursoDocumentoRepository;
        this.cursoRepository = cursoRepository;
        this.minioClient = minioClient;
        this.auditLogger = auditLogger;
        this.codigoGeneratorService = codigoGeneratorService;
    }

    /**
     * Sube un documento a MinIO y registra metadatos en BD.
     */
    public Documento subirDocumento(MultipartFile file, UUID usuarioId) {
        try {
            String objectName = UUID.randomUUID() + "_" + file.getOriginalFilename();
            String hashSha256 = HashUtils.sha256(file.getBytes());

            // Subir a MinIO
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );

            String urlStorage = String.format("/api/v1/documentos/%s/download", objectName);

            String codigo = codigoGeneratorService.generarCodigo("DOC");

            Documento documento = Documento.builder()
                    .nombreOriginal(file.getOriginalFilename())
                    .urlStorage(urlStorage)
                    .hashSha256(hashSha256)
                    .mimeType(file.getContentType())
                    .tamanoBytes(file.getSize())
                    .version(1)
                    .codigo(codigo)
                    .build();

            documento = documentoRepository.save(documento);

            auditLogger.registrarEvento(
                    "DOCUMENTO_SUBIDO",
                    usuarioId,
                    "SUBIR",
                    "DOCUMENTO",
                    documento.getId(),
                    Map.of("nombreOriginal", file.getOriginalFilename(), "tamanoBytes", file.getSize(), "hash", hashSha256, "codigo", codigo)
            );

            return documento;
        } catch (Exception e) {
            throw new RuntimeException("Error al subir documento a MinIO", e);
        }
    }

    /**
     * Sube un documento y lo asocia a un curso.
     */
    public Documento subirDocumentoParaCurso(MultipartFile file, UUID cursoId, UUID usuarioId) {
        Documento documento = subirDocumento(file, usuarioId);
        vincularDocumentoACurso(documento.getId(), cursoId);
        return documento;
    }

    /**
     * Vincula un documento existente a un curso.
     */
    public void vincularDocumentoACurso(UUID documentoId, UUID cursoId) {
        Documento documento = documentoRepository.findById(documentoId)
                .orElseThrow(() -> new IllegalArgumentException("Documento no encontrado: " + documentoId));

        com.lms.model.Curso curso = cursoRepository.getReferenceById(cursoId);

        if (!cursoDocumentoRepository.existsByCursoIdAndDocumentoId(cursoId, documentoId)) {
            CursoDocumento cd = CursoDocumento.builder()
                    .curso(curso)
                    .documento(documento)
                    .build();
            cursoDocumentoRepository.save(cd);
        }
    }

    /**
     * Lista los IDs de documentos asociados a un curso.
     */
    @Transactional(readOnly = true)
    public java.util.List<Documento> listarDocumentosPorCurso(UUID cursoId) {
        return cursoDocumentoRepository.findDocumentosByCursoId(cursoId)
                .stream()
                .map(cd -> cd.getDocumento())
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Lista todos los documentos activos.
     */
    @Transactional(readOnly = true)
    public java.util.List<Documento> listarDocumentos() {
        return documentoRepository.findAll();
    }

    /**
     * Lista documentos según el rol del usuario.
     * ADMIN/INSTRUCTOR/SUPERADMIN → todos los documentos.
     * ESTUDIANTE → solo documentos de cursos en los que está inscrito.
     */
    public java.util.List<Documento> listarDocumentosPorUsuario(UUID usuarioId, boolean esStaff) {
        if (esStaff) {
            return documentoRepository.findAll();
        }
        return documentoRepository.findByUsuarioInscripciones(usuarioId);
    }

    /**
     * Busca documento por ID.
     */
    @Transactional(readOnly = true)
    public Optional<Documento> buscarPorId(UUID id) {
        return documentoRepository.findById(id);
    }

    /**
     * Activa/desactiva un documento (toggle habilitado).
     */
    public Documento toggleHabilitado(UUID id) {
        Documento doc = documentoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Documento no encontrado: " + id));
        doc.setHabilitado(!doc.isHabilitado());
        return documentoRepository.save(doc);
    }

    /**
     * Elimina un documento de MinIO y soft-delete en BD.
     */
    public void eliminarDocumento(UUID id, UUID eliminadoPor) {
        documentoRepository.findById(id).ifPresent(documento -> {
            try {
                String objectName = documento.getUrlStorage()
                        .replace("/api/v1/documentos/", "")
                        .replace("/download", "");
                minioClient.removeObject(
                        RemoveObjectArgs.builder()
                                .bucket(bucketName)
                                .object(objectName)
                                .build()
                );
            } catch (Exception e) {
                // Log warning but continue with DB deletion
            }

            auditLogger.registrarEvento(
                    "DOCUMENTO_ELIMINADO",
                    eliminadoPor,
                    "ELIMINAR",
                    "DOCUMENTO",
                    id,
                    Map.of("nombreOriginal", documento.getNombreOriginal(), "codigo", documento.getCodigo())
            );
        });
    }

    /**
     * Obtiene el contenido de un documento desde MinIO para streaming.
     *
     * @param id ID del documento
     * @return GetObjectResponse con el contenido del archivo
     */
    public GetObjectResponse obtenerStream(UUID id) {
        Documento doc = documentoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Documento no encontrado: " + id));
        try {
            String objectName = doc.getUrlStorage()
                    .replace("/api/v1/documentos/", "")
                    .replace("/download", "");
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .build()
            );
        } catch (Exception e) {
            throw new RuntimeException("Error al obtener stream del documento", e);
        }
    }

    /**
     * Obtiene el contenido parcial de un documento (soporte Range requests).
     *
     * @param id     ID del documento
     * @param offset Byte offset de inicio
     * @param length Cantidad de bytes a leer
     * @return GetObjectResponse con el contenido parcial
     */
    public GetObjectResponse obtenerStreamParcial(UUID id, long offset, long length) {
        Documento doc = documentoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Documento no encontrado: " + id));
        try {
            String objectName = doc.getUrlStorage()
                    .replace("/api/v1/documentos/", "")
                    .replace("/download", "");
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .offset(offset)
                            .length(length)
                            .build()
            );
        } catch (Exception e) {
            throw new RuntimeException("Error al obtener stream parcial del documento", e);
        }
    }

    /**
     * Obtiene el tamaño total de un documento almacenado en MinIO.
     *
     * @param id ID del documento
     * @return Tamaño en bytes
     */
    public long obtenerTamanoTotal(UUID id) {
        Documento doc = documentoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Documento no encontrado: " + id));
        try {
            String objectName = doc.getUrlStorage()
                    .replace("/api/v1/documentos/", "")
                    .replace("/download", "");
            StatObjectResponse stat = minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .build()
            );
            return stat.size();
        } catch (Exception e) {
            return doc.getTamanoBytes();
        }
    }
}
