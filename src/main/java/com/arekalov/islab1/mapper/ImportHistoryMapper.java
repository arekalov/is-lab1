package com.arekalov.islab1.mapper;

import com.arekalov.islab1.dto.response.ImportHistoryResponseDTO;
import com.arekalov.islab1.entity.ImportHistory;
import jakarta.ejb.Stateless;

/**
 * Маппер для конвертации ImportHistory в DTO
 */
@Stateless
public class ImportHistoryMapper {
    
    /**
     * Конвертировать Entity в Response DTO
     */
    public ImportHistoryResponseDTO toResponseDTO(ImportHistory entity) {
        if (entity == null) {
            return null;
        }
        
        return ImportHistoryResponseDTO.builder()
            .id(entity.getId())
            .operationTime(entity.getOperationTime())
            .objectsCount(entity.getObjectsCount())
            .changesDescription(entity.getChangesDescription())
            .build();
    }
}

