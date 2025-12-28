package com.arekalov.islab1.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO для ответа с ошибкой
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponseDTO {
    /**
     * Сообщение об ошибке
     */
    private String message;
    
    /**
     * Код ошибки (опционально)
     */
    private String code;
    
    /**
     * Конструктор с одним сообщением
     */
    public ErrorResponseDTO(String message) {
        this.message = message;
    }
}





