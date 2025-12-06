package com.arekalov.islab1.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO для передачи данных о доме
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HouseResponseDTO {
    private Long id;
    private String name;
    private Integer year;
    private Integer numberOfFlatsOnFloor;
}

