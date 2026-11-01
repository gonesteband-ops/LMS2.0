package com.lms.service;

import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Service
public class FileUploadService {

    private final MinioClient minioClient;
    private final String bucketName;

public FileUploadService(MinioClient minioClient,
                             @Value("${app.minio.bucket-name}") String bucketName) {
        this.minioClient = minioClient;
        this.bucketName = bucketName;
    }

    public record ArchivoInfo(String url, String nombre, String mime, Long tamano) {}

    public ArchivoInfo subirArchivo(MultipartFile file, String prefijo) {
        try {
            String extension = "";
            String originalName = file.getOriginalFilename();
            if (originalName != null && originalName.contains(".")) {
                extension = originalName.substring(originalName.lastIndexOf("."));
            }
            String objectName = prefijo + "/" + UUID.randomUUID() + extension;

            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build());

            String url = "/api/archivos/" + objectName;
            return new ArchivoInfo(url, originalName, file.getContentType(), file.getSize());
        } catch (Exception e) {
            throw new RuntimeException("Error al subir archivo a MinIO", e);
        }
    }
}
