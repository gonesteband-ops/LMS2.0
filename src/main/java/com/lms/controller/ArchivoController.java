package com.lms.controller;

import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/archivos")
public class ArchivoController {

    private final MinioClient minioClient;
    private final String bucketName;

    public ArchivoController(MinioClient minioClient,
                              @Value("${app.minio.bucket-name}") String bucketName) {
        this.minioClient = minioClient;
        this.bucketName = bucketName;
    }

    @GetMapping("/**")
    public ResponseEntity<Resource> servirArchivo(HttpServletRequest request) {
        try {
            String objectName = request.getRequestURI()
                    .replaceFirst("/api/archivos/", "");
            var obj = minioClient.getObject(GetObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .build());
            var resource = new InputStreamResource(obj);
            String filename = objectName.contains("/") ? objectName.substring(objectName.lastIndexOf("/") + 1) : objectName;
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
}
