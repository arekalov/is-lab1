package com.arekalov.islab1.dto;

import jakarta.validation.constraints.NotNull;

/**
 * DTO для создания координат
 */
public class CreateCoordinatesRequest {
    
    @NotNull(message = "Координата X не может быть null")
    private Integer x;
    
    private Integer y;
    
    // Конструктор без параметров
    public CreateCoordinatesRequest() {}
    
    public CreateCoordinatesRequest(Integer x, Integer y) {
        this.x = x;
        this.y = y;
    }
    
    // Getters and Setters
    public Integer getX() { return x; }
    public void setX(Integer x) { this.x = x; }
    
    public Integer getY() { return y; }
    public void setY(Integer y) { this.y = y; }
}
