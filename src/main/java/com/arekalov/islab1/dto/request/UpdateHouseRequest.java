package com.arekalov.islab1.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO для обновления дома
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateHouseRequest {
    @NotBlank(message = "Название не может быть пустым")
    private String name;
    
    @NotNull(message = "Год не может быть null")
    @Positive(message = "Год должен быть положительным")
    private Integer year;
    
    @NotNull(message = "Количество квартир на этаже не может быть null")
    @Positive(message = "Количество квартир на этаже должно быть положительным")
    private Integer numberOfFlatsOnFloor;
}

