package com.arekalov.islab1.dto.request;

import jakarta.validation.constraints.*;
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

    @NotNull(message = "Координата Y не может быть null")
    @Min(value = -515, message = "Координата Y должна быть больше -515")
    private Integer y;
}