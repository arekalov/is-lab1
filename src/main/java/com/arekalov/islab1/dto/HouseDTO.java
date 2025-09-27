package com.arekalov.islab1.dto;

/**
 * DTO для дома
 */
public class HouseDTO {
    private Long id;
    private String name;
    private Integer year;
    private Integer numberOfFlatsOnFloor;
    
    // Конструктор без параметров
    public HouseDTO() {}
    
    public HouseDTO(Long id, String name, Integer year, Integer numberOfFlatsOnFloor) {
        this.id = id;
        this.name = name;
        this.year = year;
        this.numberOfFlatsOnFloor = numberOfFlatsOnFloor;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public Integer getYear() { return year; }
    public void setYear(Integer year) { this.year = year; }
    
    public Integer getNumberOfFlatsOnFloor() { return numberOfFlatsOnFloor; }
    public void setNumberOfFlatsOnFloor(Integer numberOfFlatsOnFloor) { this.numberOfFlatsOnFloor = numberOfFlatsOnFloor; }
}
