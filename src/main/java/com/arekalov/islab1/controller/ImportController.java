package com.arekalov.islab1.controller;

import com.arekalov.islab1.dto.response.DownloadLinkResponseDTO;
import com.arekalov.islab1.dto.response.ErrorResponseDTO;
import com.arekalov.islab1.dto.response.ImportHistoryResponseDTO;
import com.arekalov.islab1.entity.ImportHistory;
import com.arekalov.islab1.exception.UniqueConstraintViolationException;
import com.arekalov.islab1.mapper.ImportHistoryMapper;
import com.arekalov.islab1.repository.ImportHistoryRepository;
import com.arekalov.islab1.service.ImportService;
import com.arekalov.islab1.service.MinioService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * REST контроллер для операций импорта
 */
@Path("/import")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ImportController {
    
    private static final Logger logger = Logger.getLogger(ImportController.class.getName());
    
    @Inject
    private ImportService importService;
    
    @Inject
    private ImportHistoryRepository importHistoryRepository;
    
    @Inject
    private ImportHistoryMapper importHistoryMapper;
    
    @Inject
    private MinioService minioService;
    
    /**
     * Универсальный импорт объектов с сохранением в MinIO
     * POST /api/import?fileName=import.json
     * Content-Type: application/json
     * 
     * Body: JSON массив операций
     * 
     * Пример:
     * [
     *   {
     *     "type": "FLAT",
     *     "operation": "CREATE",
     *     "data": {
     *       "name": "Квартира 101",
     *       "floor": 1,
     *       "area": 50,
     *       "coordinates": { "x": 100, "y": 200 },
     *       "house": { "name": "Дом A", "year": 2023, "numberOfFlatsOnFloor": 4 }
     *     }
     *   }
     * ]
     */
    @POST
    public Response importObjects(
        String json,
        @QueryParam("fileName") @DefaultValue("import.json") String fileName
    ) {
        logger.info("ImportController.importObjects() - получен запрос на импорт");
        logger.info("File name: " + fileName);
        
        try {
            // Выполняем импорт с сохранением в MinIO (2PC)
            ImportHistory history = importService.importObjectsWithFileName(json, fileName);
            
            // Конвертируем в DTO
            ImportHistoryResponseDTO response = importHistoryMapper.toResponseDTO(history);
            
            logger.info("ImportController.importObjects() - импорт успешен, id=" + history.getId());
            
            return Response
                .status(Response.Status.CREATED)
                .entity(response)
                .build();
                
        } catch (UniqueConstraintViolationException e) {
            // Ошибка валидации уникальности данных - 400 Bad Request
            logger.warning("ImportController.importObjects() - ошибка валидации: " + e.getMessage());
            return Response
                .status(Response.Status.BAD_REQUEST)
                .entity(new ErrorResponseDTO(e.getMessage()))
                .build();
        } catch (IllegalArgumentException e) {
            // Другие ошибки валидации - 400 Bad Request
            logger.warning("ImportController.importObjects() - ошибка валидации: " + e.getMessage());
            return Response
                .status(Response.Status.BAD_REQUEST)
                .entity(new ErrorResponseDTO(e.getMessage()))
                .build();
        } catch (Exception e) {
            // Технические ошибки (БД, MinIO, и т.д.) - 500 Internal Server Error
            logger.severe("ImportController.importObjects() - техническая ошибка: " + e.getMessage());
            throw new RuntimeException("Ошибка импорта: " + e.getMessage(), e);
        }
    }
    
    /**
     * Скачать файл импорта из MinIO
     * GET /api/import/history/{id}/download
     */
    @GET
    @Path("/history/{id}/download")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response downloadImportFile(@PathParam("id") Long id) {
        logger.info("ImportController.downloadImportFile() - id=" + id);
        
        try {
            ImportHistory history = importHistoryRepository.findById(id);
            
            if (history == null) {
                return Response
                    .status(Response.Status.NOT_FOUND)
                    .entity(new ErrorMessage("История импорта не найдена"))
                    .build();
            }
            
            String objectKey = history.getFileObjectKey();
            if (objectKey == null || objectKey.isEmpty()) {
                return Response
                    .status(Response.Status.NOT_FOUND)
                    .entity(new ErrorMessage("Файл не найден для этой записи истории"))
                    .build();
            }
            
            // Скачиваем файл из MinIO
            byte[] fileContent = minioService.downloadFile(objectKey);
            
            // Возвращаем файл с оригинальным расширением
            String fileName = "import-" + id + ".json";
            
            return Response
                .ok(fileContent)
                .header("Content-Disposition", "attachment; filename=\"" + fileName + "\"")
                .header("Content-Type", "application/json")
                .build();
                
        } catch (Exception e) {
            logger.severe("ImportController.downloadImportFile() - ошибка: " + e.getMessage());
            return Response
                .status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ErrorMessage("Ошибка скачивания файла: " + e.getMessage()))
                .build();
        }
    }
    
    /**
     * Получить presigned URL для скачивания файла импорта
     * GET /api/import/history/{id}/download-link
     */
    @GET
    @Path("/history/{id}/download-link")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDownloadLink(@PathParam("id") Long id) {
        logger.info("ImportController.getDownloadLink() - id=" + id);
        
        try {
            ImportHistory history = importHistoryRepository.findById(id);
            
            if (history == null) {
                return Response
                    .status(Response.Status.NOT_FOUND)
                    .entity(new ErrorMessage("История импорта не найдена"))
                    .build();
            }
            
            String objectKey = history.getFileObjectKey();
            if (objectKey == null || objectKey.isEmpty()) {
                return Response
                    .status(Response.Status.NOT_FOUND)
                    .entity(new ErrorMessage("Файл не найден для этой записи истории"))
                    .build();
            }
            
            // Генерируем presigned URL (действует 5 минут)
            int expirySeconds = 300; // 5 минут
            String presignedUrl = minioService.getPresignedUrl(objectKey, expirySeconds);
            
            DownloadLinkResponseDTO response = new DownloadLinkResponseDTO(presignedUrl, expirySeconds);
            
            logger.info("ImportController.getDownloadLink() - presigned URL generated");
            
            return Response.ok(response).build();
                
        } catch (Exception e) {
            logger.severe("ImportController.getDownloadLink() - ошибка: " + e.getMessage());
            return Response
                .status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ErrorMessage("Ошибка генерации ссылки: " + e.getMessage()))
                .build();
        }
    }
    
    /**
     * Получить историю импорта
     * GET /api/import/history?page=0&size=10
     */
    @GET
    @Path("/history")
    public Response getImportHistory(
        @QueryParam("page") @DefaultValue("0") int page,
        @QueryParam("size") @DefaultValue("10") int size
    ) {
        logger.info("ImportController.getImportHistory() - page=" + page + ", size=" + size);
        
        try {
            List<ImportHistory> history = importHistoryRepository.findAll(page, size);
            long total = importHistoryRepository.count();
            
            List<ImportHistoryResponseDTO> items = history.stream()
                .map(importHistoryMapper::toResponseDTO)
                .collect(Collectors.toList());
            
            PagedResponse<ImportHistoryResponseDTO> response = new PagedResponse<>(
                items, total, page, size
            );
            
            return Response.ok(response).build();
            
        } catch (Exception e) {
            logger.severe("ImportController.getImportHistory() - ошибка: " + e.getMessage());
            return Response
                .status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ErrorMessage("Ошибка получения истории: " + e.getMessage()))
                .build();
        }
    }
    
    /**
     * Получить последние N записей истории
     * GET /api/import/history/latest?limit=5
     */
    @GET
    @Path("/history/latest")
    public Response getLatestImportHistory(
        @QueryParam("limit") @DefaultValue("5") int limit
    ) {
        logger.info("ImportController.getLatestImportHistory() - limit=" + limit);
        
        try {
            List<ImportHistory> history = importHistoryRepository.findLatest(limit);
            
            List<ImportHistoryResponseDTO> items = history.stream()
                .map(importHistoryMapper::toResponseDTO)
                .collect(Collectors.toList());
            
            return Response.ok(items).build();
            
        } catch (Exception e) {
            logger.severe("ImportController.getLatestImportHistory() - ошибка: " + e.getMessage());
            return Response
                .status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ErrorMessage("Ошибка получения истории: " + e.getMessage()))
                .build();
        }
    }
    
    // Вспомогательные классы для ответов
    
    private static class ErrorMessage {
        public String message;
        
        public ErrorMessage(String message) {
            this.message = message;
        }
    }
    
    private static class PagedResponse<T> {
        public List<T> items;
        public long total;
        public int page;
        public int size;
        
        public PagedResponse(List<T> items, long total, int page, int size) {
            this.items = items;
            this.total = total;
            this.page = page;
            this.size = size;
        }
    }
}


