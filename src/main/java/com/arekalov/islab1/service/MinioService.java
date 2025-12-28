package com.arekalov.islab1.service;

import com.arekalov.islab1.config.MinioConfig;
import io.minio.*;
import io.minio.http.Method;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Сервис для работы с MinIO (S3-compatible storage)
 * 
 * Поддержка двухфазного коммита с staging/final областями:
 * - staging/ - временные файлы (PREPARE phase)
 * - final/ - подтвержденные файлы (COMMIT phase)
 * 
 * Основные операции:
 * - uploadToStaging() - загрузка в staging (PREPARE)
 * - copyToFinal() - копирование staging -> final (COMMIT)
 * - deleteStaging() - удаление staging файла (COMMIT/ABORT)
 */
@ApplicationScoped
public class MinioService {
    
    private static final Logger logger = Logger.getLogger(MinioService.class.getName());
    
    private static final String BUCKET_NAME = MinioConfig.getBucketName();
    private static final String STAGING_PREFIX = "staging/";
    private static final String FINAL_PREFIX = "final/";
    
    @Inject
    private MinioClient minioClient;
    
    /**
     * Инициализация - создание bucket если не существует
     */
    public void ensureBucketExists() {
        try {
            boolean exists = minioClient.bucketExists(
                BucketExistsArgs.builder()
                    .bucket(BUCKET_NAME)
                    .build()
            );
            
            if (!exists) {
                minioClient.makeBucket(
                    MakeBucketArgs.builder()
                        .bucket(BUCKET_NAME)
                        .build()
                );
                logger.info("MinIO bucket created: " + BUCKET_NAME);
            } else {
                logger.info("MinIO bucket already exists: " + BUCKET_NAME);
            }
        } catch (Exception e) {
            logger.severe("Failed to ensure bucket exists: " + e.getMessage());
            throw new RuntimeException("MinIO bucket initialization failed", e);
        }
    }
    
    /**
     * Загрузить файл в MinIO
     * 
     * @param fileContent содержимое файла
     * @param originalFileName оригинальное имя файла (для расширения)
     * @param contentType MIME тип файла
     * @return objectKey - уникальный идентификатор файла в MinIO (UUID.json)
     */
    public String uploadFile(byte[] fileContent, String originalFileName, String contentType) {
        try {
            // Генерируем уникальное имя файла: UUID + расширение
            String extension = getFileExtension(originalFileName);
            String objectKey = UUID.randomUUID().toString() + extension;
            
            logger.info("Uploading file to MinIO:");
            logger.info("  Original name: " + originalFileName);
            logger.info("  Object key: " + objectKey);
            logger.info("  Size: " + fileContent.length + " bytes");
            logger.info("  Bucket: " + BUCKET_NAME);
            
            // Убеждаемся что bucket существует
            ensureBucketExists();
            
            // Загружаем файл
            minioClient.putObject(
                PutObjectArgs.builder()
                    .bucket(BUCKET_NAME)
                    .object(objectKey)
                    .stream(new ByteArrayInputStream(fileContent), fileContent.length, -1)
                    .contentType(contentType)
                    .build()
            );
            
            logger.info("File uploaded successfully: " + objectKey);
            return objectKey;
            
        } catch (Exception e) {
            logger.severe("Failed to upload file to MinIO: " + e.getMessage());
            throw new RuntimeException("MinIO upload failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Скачать файл из MinIO
     * 
     * @param objectKey идентификатор файла в MinIO
     * @return содержимое файла
     */
    public byte[] downloadFile(String objectKey) {
        try {
            logger.info("Downloading file from MinIO: " + objectKey);
            
            try (InputStream stream = minioClient.getObject(
                GetObjectArgs.builder()
                    .bucket(BUCKET_NAME)
                    .object(objectKey)
                    .build()
            )) {
                byte[] content = stream.readAllBytes();
                logger.info("File downloaded successfully: " + objectKey + " (" + content.length + " bytes)");
                return content;
            }
            
        } catch (Exception e) {
            logger.severe("Failed to download file from MinIO: " + e.getMessage());
            throw new RuntimeException("MinIO download failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Удалить файл из MinIO
     * 
     * @param objectKey идентификатор файла в MinIO
     */
    public void deleteFile(String objectKey) {
        try {
            logger.info("Deleting file from MinIO: " + objectKey);
            
            minioClient.removeObject(
                RemoveObjectArgs.builder()
                    .bucket(BUCKET_NAME)
                    .object(objectKey)
                    .build()
            );
            
            logger.info("File deleted successfully: " + objectKey);
            
        } catch (Exception e) {
            logger.severe("Failed to delete file from MinIO: " + e.getMessage());
            throw new RuntimeException("MinIO delete failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Проверить существование файла
     * 
     * @param objectKey идентификатор файла
     * @return true если файл существует
     */
    public boolean fileExists(String objectKey) {
        try {
            minioClient.statObject(
                StatObjectArgs.builder()
                    .bucket(BUCKET_NAME)
                    .object(objectKey)
                    .build()
            );
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Получить presigned URL для скачивания файла из MinIO
     * 
     * @param objectKey ключ объекта в MinIO
     * @param expirySeconds время действия ссылки в секундах (по умолчанию 5 минут)
     * @return presigned URL для прямого скачивания
     */
    public String getPresignedUrl(String objectKey, int expirySeconds) {
        try {
            logger.info("Generating presigned URL for: " + objectKey);
            logger.info("Expiry: " + expirySeconds + " seconds");
            
            // Создаем Map с extra query параметрами для Content-Disposition
            java.util.Map<String, String> extraQueryParams = new java.util.HashMap<>();
            extraQueryParams.put("response-content-disposition", "attachment; filename=\"" + objectKey + "\"");
            
            String url = minioClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(BUCKET_NAME)
                    .object(objectKey)
                    .expiry(expirySeconds)
                    .extraQueryParams(extraQueryParams)
                    .build()
            );
            
            logger.info("Presigned URL generated successfully");
            return url;
            
        } catch (Exception e) {
            logger.severe("Failed to generate presigned URL for: " + objectKey + " - " + e.getMessage());
            throw new RuntimeException("Не удалось сгенерировать ссылку для скачивания", e);
        }
    }
    
    // ===============================================
    // 2PC Methods: staging/final operations
    // ===============================================
    
    /**
     * PHASE 1 (PREPARE): Загрузить файл в staging область
     * Файл считается "неподтвержденным" пока не будет скопирован в final/
     * 
     * @param fileContent содержимое файла
     * @param transactionId ID транзакции (будет использован в имени файла)
     * @param originalFileName оригинальное имя файла
     * @return objectKey в формате "staging/{transactionId}.json"
     */
    public String uploadToStaging(byte[] fileContent, String transactionId, String originalFileName) {
        try {
            String extension = getFileExtension(originalFileName);
            String objectKey = STAGING_PREFIX + transactionId + extension;
            
            logger.info("2PC PREPARE: Uploading to staging area");
            logger.info("  Transaction ID: " + transactionId);
            logger.info("  Object key: " + objectKey);
            logger.info("  Size: " + fileContent.length + " bytes");
            
            ensureBucketExists();
            
            minioClient.putObject(
                PutObjectArgs.builder()
                    .bucket(BUCKET_NAME)
                    .object(objectKey)
                    .stream(new ByteArrayInputStream(fileContent), fileContent.length, -1)
                    .contentType("application/json")
                    .build()
            );
            
            logger.info("2PC PREPARE: File uploaded to staging successfully");
            return objectKey;
            
        } catch (Exception e) {
            logger.severe("2PC PREPARE FAILED: Cannot upload to staging: " + e.getMessage());
            throw new RuntimeException("MinIO staging upload failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * PHASE 2 (COMMIT): Скопировать файл из staging в final
     * Это "публикация" файла - после этого он считается зафиксированным
     * 
     * @param stagingObjectKey путь к staging файлу (staging/{uuid}.json)
     * @param transactionId ID транзакции
     * @return objectKey в формате "final/{transactionId}.json"
     */
    public String copyToFinal(String stagingObjectKey, String transactionId) {
        try {
            String extension = stagingObjectKey.substring(stagingObjectKey.lastIndexOf('.'));
            String finalObjectKey = FINAL_PREFIX + transactionId + extension;
            
            logger.info("2PC COMMIT: Copying from staging to final");
            logger.info("  From: " + stagingObjectKey);
            logger.info("  To: " + finalObjectKey);
            
            // Копируем файл: staging -> final
            minioClient.copyObject(
                CopyObjectArgs.builder()
                    .bucket(BUCKET_NAME)
                    .object(finalObjectKey)
                    .source(
                        CopySource.builder()
                            .bucket(BUCKET_NAME)
                            .object(stagingObjectKey)
                            .build()
                    )
                    .build()
            );
            
            logger.info("2PC COMMIT: File copied to final successfully");
            return finalObjectKey;
            
        } catch (Exception e) {
            logger.severe("2PC COMMIT FAILED: Cannot copy to final: " + e.getMessage());
            throw new RuntimeException("MinIO copy to final failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * PHASE 2 (COMMIT/ABORT): Удалить staging файл
     * Вызывается после успешного COMMIT (очистка) или при ABORT (компенсация)
     * 
     * @param stagingObjectKey путь к staging файлу
     */
    public void deleteStaging(String stagingObjectKey) {
        try {
            logger.info("2PC CLEANUP: Deleting staging file: " + stagingObjectKey);
            
            minioClient.removeObject(
                RemoveObjectArgs.builder()
                    .bucket(BUCKET_NAME)
                    .object(stagingObjectKey)
                    .build()
            );
            
            logger.info("2PC CLEANUP: Staging file deleted successfully");
            
        } catch (Exception e) {
            logger.severe("2PC CLEANUP FAILED: Cannot delete staging file: " + e.getMessage());
            // Не пробрасываем исключение - это некритично
            // Recovery job почистит позже
        }
    }
    
    /**
     * Проверить существование staging файла
     */
    public boolean stagingFileExists(String stagingObjectKey) {
        return fileExists(stagingObjectKey);
    }
    
    /**
     * Проверить существование final файла
     */
    public boolean finalFileExists(String finalObjectKey) {
        return fileExists(finalObjectKey);
    }
    
    /**
     * Получить список всех staging файлов (для cleanup/recovery)
     */
    public java.util.List<String> listStagingFiles() {
        try {
            java.util.List<String> stagingFiles = new java.util.ArrayList<>();
            
            Iterable<io.minio.Result<io.minio.messages.Item>> results = minioClient.listObjects(
                io.minio.ListObjectsArgs.builder()
                    .bucket(BUCKET_NAME)
                    .prefix(STAGING_PREFIX)
                    .recursive(true)
                    .build()
            );
            
            for (io.minio.Result<io.minio.messages.Item> result : results) {
                io.minio.messages.Item item = result.get();
                stagingFiles.add(item.objectName());
            }
            
            return stagingFiles;
            
        } catch (Exception e) {
            logger.warning("Failed to list staging files: " + e.getMessage());
            return java.util.Collections.emptyList();
        }
    }
    
    /**
     * Извлечь расширение файла из имени
     */
    private String getFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return ".json";  // По умолчанию JSON
        }
        return fileName.substring(fileName.lastIndexOf("."));
    }
}

