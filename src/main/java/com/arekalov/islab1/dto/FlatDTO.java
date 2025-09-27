package com.arekalov.islab1.dto;

import com.arekalov.islab1.entity.Furnish;
import com.arekalov.islab1.entity.View;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.ZonedDateTime;

/**
 * DTO для передачи данных о квартире
 */
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
    
    // Конструктор без параметров
    public FlatDTO() {}
    
    public FlatDTO(Long id, String name, CoordinatesDTO coordinates, ZonedDateTime creationDate,
                   Long area, Long price, Boolean balcony, Long timeToMetroOnFoot,
                   Integer numberOfRooms, Long livingSpace, Furnish furnish, View view, HouseDTO house) {
        this.id = id;
        this.name = name;
        this.coordinates = coordinates;
        this.creationDate = creationDate;
        this.area = area;
        this.price = price;
        this.balcony = balcony;
        this.timeToMetroOnFoot = timeToMetroOnFoot;
        this.numberOfRooms = numberOfRooms;
        this.livingSpace = livingSpace;
        this.furnish = furnish;
        this.view = view;
        this.house = house;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public CoordinatesDTO getCoordinates() { return coordinates; }
    public void setCoordinates(CoordinatesDTO coordinates) { this.coordinates = coordinates; }
    
    public ZonedDateTime getCreationDate() { return creationDate; }
    public void setCreationDate(ZonedDateTime creationDate) { this.creationDate = creationDate; }
    
    public Long getArea() { return area; }
    public void setArea(Long area) { this.area = area; }
    
    public Long getPrice() { return price; }
    public void setPrice(Long price) { this.price = price; }
    
    public Boolean getBalcony() { return balcony; }
    public void setBalcony(Boolean balcony) { this.balcony = balcony; }
    
    public Long getTimeToMetroOnFoot() { return timeToMetroOnFoot; }
    public void setTimeToMetroOnFoot(Long timeToMetroOnFoot) { this.timeToMetroOnFoot = timeToMetroOnFoot; }
    
    public Integer getNumberOfRooms() { return numberOfRooms; }
    public void setNumberOfRooms(Integer numberOfRooms) { this.numberOfRooms = numberOfRooms; }
    
    public Long getLivingSpace() { return livingSpace; }
    public void setLivingSpace(Long livingSpace) { this.livingSpace = livingSpace; }
    
    public Furnish getFurnish() { return furnish; }
    public void setFurnish(Furnish furnish) { this.furnish = furnish; }
    
    public View getView() { return view; }
    public void setView(View view) { this.view = view; }
    
    public HouseDTO getHouse() { return house; }
    public void setHouse(HouseDTO house) { this.house = house; }
}
