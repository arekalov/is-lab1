package com.arekalov.islab1.controller;

import com.arekalov.islab1.dto.response.ImportHistoryResponseDTO;
import com.arekalov.islab1.entity.ImportHistory;
import com.arekalov.islab1.mapper.ImportHistoryMapper;
import com.arekalov.islab1.repository.ImportHistoryRepository;
import com.arekalov.islab1.service.ImportService;
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
    
    /**
     * Универсальный импорт объектов
     * POST /api/import
     * 
     * Body: JSON массив операций с разными типами объектов
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
     *   },
     *   {
     *     "type": "HOUSE",
     *     "operation": "UPDATE",
     *     "data": { "id": 1, "name": "Обновленный дом", "year": 2024, "numberOfFlatsOnFloor": 5 }
     *   },
     *   {
     *     "type": "FLAT",
     *     "operation": "DELETE",
     *     "data": { "id": 5 }
     *   }
     * ]
     */
    @POST
    public Response importObjects(String json) {
        logger.info("ImportController.importObjects() - получен запрос на универсальный импорт");
        
        // Выполняем импорт (исключения будут обработаны ValidationExceptionMapper)
            ImportHistory history = importService.importObjects(json);
            
            // Конвертируем в DTO
            ImportHistoryResponseDTO response = importHistoryMapper.toResponseDTO(history);
            
            logger.info("ImportController.importObjects() - импорт успешен, id=" + history.getId());
            
            return Response
                .status(Response.Status.CREATED)
                .entity(response)
                .build();
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


