package com.arekalov.islab1.config;

import io.minio.MinioClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Produces;

import java.util.logging.Logger;

/**
 * Конфигурация MinIO клиента
 * 
 * Параметры из переменных окружения:
 * - MINIO_ENDPOINT (default: http://localhost:9000)
 * - MINIO_ACCESS_KEY (default: admin)
 * - MINIO_SECRET_KEY (default: admin12345)
 * - MINIO_BUCKET (default: import-files)
 */
@ApplicationScoped
public class MinioConfig {
    
    private static final Logger logger = Logger.getLogger(MinioConfig.class.getName());
    
    private static final String DEFAULT_ENDPOINT = "http://localhost:9000";
    private static final String DEFAULT_ACCESS_KEY = "admin";
    private static final String DEFAULT_SECRET_KEY = "admin12345";
    
    /**
     * Producer для MinioClient.
     * Использует @Dependent scope, потому что MinioClient не имеет no-args конструктора
     * и не может быть проксирован CDI.
     */
    @Produces
    @Dependent
    public MinioClient minioClient() {
        String endpoint = System.getenv().getOrDefault("MINIO_ENDPOINT", DEFAULT_ENDPOINT);
        String accessKey = System.getenv().getOrDefault("MINIO_ACCESS_KEY", DEFAULT_ACCESS_KEY);
        String secretKey = System.getenv().getOrDefault("MINIO_SECRET_KEY", DEFAULT_SECRET_KEY);
        
        logger.info("Initializing MinIO client:");
        logger.info("  Endpoint: " + endpoint);
        logger.info("  Access Key: " + accessKey);
        
        return MinioClient.builder()
            .endpoint(endpoint)
            .credentials(accessKey, secretKey)
            .build();
    }
    
    /**
     * Получить имя bucket из переменной окружения
     */
    public static String getBucketName() {
        return System.getenv().getOrDefault("MINIO_BUCKET", "import-files");
    }
}

