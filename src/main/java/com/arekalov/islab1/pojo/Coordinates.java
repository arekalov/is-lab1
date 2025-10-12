package com.arekalov.islab1.pojo;

import jakarta.validation.constraints.*;
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

    @NotNull(message = "Координата X не может быть null")
    private Integer x;

    @NotNull(message = "Координата Y не может быть null")
    @Min(value = -515, message = "Координата Y должна быть больше -515")
    private Integer y;
    
    public Coordinates(Integer x, Integer y) {
        this.x = x;
        this.y = y;
    }
}