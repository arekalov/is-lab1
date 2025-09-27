package com.arekalov.islab1.dto;

import java.time.Instant;
import java.util.List;

/**
 * DTO для ответа об ошибке
 */
public class ErrorResponse {
    private String message;
    private List<String> details;
    private String timestamp;
    
    // Конструктор без параметров
    public ErrorResponse() {
        this.timestamp = Instant.now().toString();
    }
    
    public ErrorResponse(String message) {
        this.message = message;
        this.timestamp = Instant.now().toString();
    }
    
    public ErrorResponse(String message, List<String> details) {
        this.message = message;
        this.details = details;
        this.timestamp = Instant.now().toString();
    }
    
    // Getters and Setters
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    public List<String> getDetails() { return details; }
    public void setDetails(List<String> details) { this.details = details; }
    
    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
}
