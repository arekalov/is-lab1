package com.arekalov.islab1.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO для создания координат
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateCoordinatesRequest {
    @NotNull(message = "Координата X не может быть null")
    private Integer x;
    private Integer y;
}