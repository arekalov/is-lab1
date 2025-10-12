package com.arekalov.islab1.dto;

import com.arekalov.islab1.entity.Furnish;
import com.arekalov.islab1.entity.View;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.ZonedDateTime;

/**
 * DTO для передачи данных о квартире
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FlatDTO {
    private Long id;
    private String name;
    private CoordinatesDTO coordinates;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    private ZonedDateTime creationDate;
    
    private Long area;
    private Long price;
    private Boolean balcony;
    private Long timeToMetroOnFoot;
    private Integer numberOfRooms;
    private Long livingSpace;
    private Furnish furnish;
    private View view;
    private HouseDTO house;
}