package com.arekalov.islab1.dto.response;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;
import java.util.List;

/**
 * DTO для ответа об ошибке
 */
@Data
@NoArgsConstructor
public class ErrorResponse {
    private String message;
    private List<String> details;
    private String timestamp;
    
    public ErrorResponse(String message) {
        this.message = message;
        this.timestamp = Instant.now().toString();
    }
    
    public ErrorResponse(String message, List<String> details) {
        this.message = message;
        this.details = details;
        this.timestamp = Instant.now().toString();
    }
}