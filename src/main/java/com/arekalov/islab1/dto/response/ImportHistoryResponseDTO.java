package com.arekalov.islab1.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * DTO для ответа с информацией об истории импорта
 * Содержит только успешные операции
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImportHistoryResponseDTO {
    
    /**
     * ID операции импорта
     */
    private Long id;
    
    /**
     * Время выполнения операции
     */
    private LocalDateTime operationTime;
    
    /**
     * Количество успешно импортированных объектов
     */
    private Integer objectsCount;
    
    /**
     * UUID файла в MinIO (import-files bucket)
     * Используется для скачивания оригинального JSON файла импорта
     */
    private String fileObjectKey;
}

