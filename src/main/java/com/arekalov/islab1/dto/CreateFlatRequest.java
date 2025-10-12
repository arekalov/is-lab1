package com.arekalov.islab1.dto;

import com.arekalov.islab1.entity.Furnish;
import com.arekalov.islab1.entity.View;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO для создания новой квартиры
 */
@Data
@NoArgsConstructor
public class CreateFlatRequest {
    @NotBlank(message = "Название не может быть пустым")
    private String name;
    
    @NotNull(message = "Координаты не могут быть null")
    private CreateCoordinatesRequest coordinates;
    
    @Min(value = 1, message = "Площадь должна быть больше 0")
    private Long area;
    
    @Min(value = 1, message = "Цена должна быть больше 0")
    @Max(value = 581208244, message = "Максимальная цена: 581208244")
    private Long price;
    
    private Boolean balcony;
    
    @Min(value = 1, message = "Время до метро должно быть больше 0")
    private Long timeToMetroOnFoot;
    
    @Min(value = 1, message = "Количество комнат должно быть больше 0")
    @Max(value = 13, message = "Максимальное количество комнат: 13")
    private Integer numberOfRooms;
    
    @Min(value = 1, message = "Жилая площадь должна быть больше 0")
    private Long livingSpace;
    
    @NotNull(message = "Тип мебели не может быть null")
    private Furnish furnish;
    
    @NotNull(message = "Вид из окна не может быть null")
    private View view;
    
    private Long houseId;
}