package com.arekalov.islab1.dto;

import jakarta.validation.constraints.Min;

/**
 * DTO для создания дома
 */
public class CreateHouseRequest {
    
    private String name;
    
    @Min(value = 1, message = "Год постройки должен быть больше 0")
    private Integer year;
    
    @Min(value = 1, message = "Количество квартир на этаже должно быть больше 0")
    private Integer numberOfFlatsOnFloor;
    
    // Конструктор без параметров
    public CreateHouseRequest() {}
    
    public CreateHouseRequest(String name, Integer year, Integer numberOfFlatsOnFloor) {
        this.name = name;
        this.year = year;
        this.numberOfFlatsOnFloor = numberOfFlatsOnFloor;
    }
    
    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public Integer getYear() { return year; }
    public void setYear(Integer year) { this.year = year; }
    
    public Integer getNumberOfFlatsOnFloor() { return numberOfFlatsOnFloor; }
    public void setNumberOfFlatsOnFloor(Integer numberOfFlatsOnFloor) { this.numberOfFlatsOnFloor = numberOfFlatsOnFloor; }
}
