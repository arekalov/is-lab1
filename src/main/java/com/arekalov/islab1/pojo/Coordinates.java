package com.arekalov.islab1.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Координаты
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Coordinates {
    private Long id;
    private Integer x;
    private Integer y;
    
    public Coordinates(Integer x, Integer y) {
        this.x = x;
        this.y = y;
    }
}
