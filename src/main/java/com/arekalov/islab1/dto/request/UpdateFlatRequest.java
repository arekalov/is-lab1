package com.arekalov.islab1.dto.request;

import com.arekalov.islab1.entity.Furnish;
import com.arekalov.islab1.entity.View;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO для обновления квартиры
 * Все поля обязательны, так как это полное обновление объекта (PUT)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateFlatRequest {
    @NotBlank(message = "Название не может быть пустым")
    private String name;
    
    @NotNull(message = "Координаты не могут быть null")
    @Valid
    private CreateCoordinatesRequest coordinates;
    
    @NotNull(message = "Площадь не может быть null")
    @Positive(message = "Площадь должна быть больше 0")
    private Long area;
    
    @NotNull(message = "Цена не может быть null")
    @Positive(message = "Цена должна быть больше 0")
    @Max(value = 581208244, message = "Максимальная цена: 581208244")
    private Long price;
    
    private Boolean balcony;
    
    @NotNull(message = "Время до метро не может быть null")
    @Positive(message = "Время до метро должно быть больше 0")
    private Long timeToMetroOnFoot;
    
    @NotNull(message = "Количество комнат не может быть null")
    @Min(value = 1, message = "Количество комнат должно быть больше 0")
    @Max(value = 13, message = "Максимальное количество комнат: 13")
    private Integer numberOfRooms;
    
    @NotNull(message = "Жилая площадь не может быть null")
    @Positive(message = "Жилая площадь должна быть больше 0")
    private Long livingSpace;
    
    @NotNull(message = "Тип мебели не может быть null")
    private Furnish furnish;
    
    @NotNull(message = "Вид из окна не может быть null")
    private View view;
    
    // Этаж сделан необязательным для совместимости с фронтендом
    // Если не передан, будет использован этаж из существующей квартиры
    @Positive(message = "Этаж должен быть больше 0")
    private Integer floor;
    
    private Long houseId;
}

