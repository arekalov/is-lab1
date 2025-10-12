package com.arekalov.islab1.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Дом
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class House {
    private Long id;
    private String name;
    private Integer year;
    private Integer numberOfFlatsOnFloor;
    
    public House(String name, Integer year, Integer numberOfFlatsOnFloor) {
        this.name = name;
        this.year = year;
        this.numberOfFlatsOnFloor = numberOfFlatsOnFloor;
    }
}
