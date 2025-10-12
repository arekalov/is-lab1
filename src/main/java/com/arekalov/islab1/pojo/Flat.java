package com.arekalov.islab1.pojo;

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
    private String name;
    private Coordinates coordinates;
    private ZonedDateTime creationDate;
    private Long area;
    private Long price;
    private Boolean balcony;
    private Long timeToMetroOnFoot;
    private Integer numberOfRooms;
    private Long livingSpace;
    private Furnish furnish;
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
