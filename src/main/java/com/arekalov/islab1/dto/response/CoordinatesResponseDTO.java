package com.arekalov.islab1.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO для передачи данных о координатах
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CoordinatesResponseDTO {
    private Long id;
    private Integer x;
    private Integer y;
}

