package com.arekalov.islab1.dto.request;

import jakarta.validation.constraints.*;
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
    @NotBlank(message = "Название не может быть пустым")
    private String name;
    
    @NotNull(message = "Год постройки не может быть null")
    @Min(value = 1, message = "Год постройки должен быть больше 0")
    private Integer year;
    
    @NotNull(message = "Количество квартир на этаже не может быть null")
    @Min(value = 1, message = "Количество квартир на этаже должно быть больше 0")
    private Integer numberOfFlatsOnFloor;
}