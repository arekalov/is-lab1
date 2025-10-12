package com.arekalov.islab1.dto;

import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO для создания дома
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateHouseRequest {
    private String name;
    
    @Min(value = 1, message = "Год постройки должен быть больше 0")
    private Integer year;
    
    @Min(value = 1, message = "Количество квартир на этаже должно быть больше 0")
    private Integer numberOfFlatsOnFloor;
}