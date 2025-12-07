package com.arekalov.islab1.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO для операции импорта
 * Описывает одну операцию над объектом
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ImportOperationRequest {
    
    /**
     * Тип объекта: FLAT, HOUSE, COORDINATES
     */
    private String type;
    
    /**
     * Операция: CREATE, UPDATE, DELETE
     * Если не указана, определяется автоматически по наличию id:
     * - есть id → UPDATE
     * - нет id → CREATE
     */
    private String operation;
    
    /**
     * Данные объекта в формате JSON
     * Для CREATE/UPDATE: полный объект с полями
     * Для DELETE: объект с полем id
     */
    private Object data;
}

