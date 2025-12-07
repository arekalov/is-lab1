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
     * Описание изменений в формате JSON
     * Массив объектов с информацией о созданных сущностях
     */
    private String changesDescription;
}

