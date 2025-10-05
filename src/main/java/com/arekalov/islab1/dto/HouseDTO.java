package com.arekalov.islab1.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO для дома
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HouseDTO {
    private Long id;
    private String name;
    private Integer year;
    private Integer numberOfFlatsOnFloor;
}
