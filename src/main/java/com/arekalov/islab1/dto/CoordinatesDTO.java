package com.arekalov.islab1.dto;

/**
 * DTO для координат
 */
public class CoordinatesDTO {
    private Long id;
    private Integer x;
    private Integer y;
    
    // Конструктор без параметров
    public CoordinatesDTO() {}
    
    public CoordinatesDTO(Long id, Integer x, Integer y) {
        this.id = id;
        this.x = x;
        this.y = y;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Integer getX() { return x; }
    public void setX(Integer x) { this.x = x; }
    
    public Integer getY() { return y; }
    public void setY(Integer y) { this.y = y; }
}
