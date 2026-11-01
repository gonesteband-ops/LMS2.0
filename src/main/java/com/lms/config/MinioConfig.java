package com.lms.config;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;

/**
 * Configuración del cliente MinIO para Object Storage (S3-compatible).
 * La BD solo almacena metadatos; los archivos físicos van aquí.
 * Auto-crea el bucket si no existe al iniciar la aplicación.
 */
@Configuration
public class MinioConfig {

    private static final Logger log = LoggerFactory.getLogger(MinioConfig.class);

    @Value("${app.minio.endpoint}")
    private String endpoint;

    @Value("${app.minio.access-key}")
    private String accessKey;

    @Value("${app.minio.secret-key}")
    private String secretKey;

    @Value("${app.minio.bucket-name}")
    private String bucketName;

    @Autowired
    private BeanFactory beanFactory;

    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
    }

    /**
     * Verifica que el bucket exista en MinIO al iniciar la aplicación.
     * Se ejecuta cuando la aplicación está lista (ApplicationReadyEvent),
     * después de que todos los beans estén inicializados.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void inicializarBucket() {
        try {
            MinioClient client = beanFactory.getBean(MinioClient.class);
            boolean existe = client.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());

            if (!existe) {
                client.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
                log.info("✅ Bucket '{}' creado exitosamente en MinIO ({})", bucketName, endpoint);
            } else {
                log.info("✅ Bucket '{}' ya existe en MinIO ({})", bucketName, endpoint);
            }
        } catch (Exception e) {
            log.error("❌ No se pudo verificar/crear el bucket '{}' en MinIO: {}", bucketName, e.getMessage());
        }
    }
}
