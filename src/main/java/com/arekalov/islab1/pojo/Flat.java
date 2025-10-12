package com.arekalov.islab1.pojo;

import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.ZonedDateTime;

/**
 * Квартира
 */
@Data
@NoArgsConstructor
public class Flat {
    private Long id;

    @NotBlank(message = "Название не может быть пустым")
    private String name;

    @NotNull(message = "Координаты не могут быть null")
    private Coordinates coordinates;

    @NotNull(message = "Дата создания не может быть null")
    private ZonedDateTime creationDate;

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

    private House house;
    
    public Flat(String name, Coordinates coordinates, Long area, Long price, 
                Long timeToMetroOnFoot, Integer numberOfRooms, Long livingSpace, 
                Furnish furnish, View view) {
        this.name = name;
        this.coordinates = coordinates;
        this.creationDate = ZonedDateTime.now();
        this.area = area;
        this.price = price;
        this.timeToMetroOnFoot = timeToMetroOnFoot;
        this.numberOfRooms = numberOfRooms;
        this.livingSpace = livingSpace;
        this.furnish = furnish;
        this.view = view;
    }
}