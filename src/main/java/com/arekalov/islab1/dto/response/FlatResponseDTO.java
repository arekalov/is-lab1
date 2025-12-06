package com.arekalov.islab1.dto.response;

import com.arekalov.islab1.entity.Furnish;
import com.arekalov.islab1.entity.View;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * Response DTO для передачи данных о квартире
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FlatResponseDTO {
    private Long id;
    private String name;
    private CoordinatesResponseDTO coordinates;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime creationDate;
    
    private Long area;
    private Long price;
    private Boolean balcony;
    private Long timeToMetroOnFoot;
    private Integer numberOfRooms;
    private Long livingSpace;
    private Furnish furnish;
    private View view;
    private Integer floor;
    private HouseResponseDTO house;
}

