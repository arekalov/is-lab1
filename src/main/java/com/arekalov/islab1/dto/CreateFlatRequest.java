package com.arekalov.islab1.dto;

import com.arekalov.islab1.entity.Furnish;
import com.arekalov.islab1.entity.View;
import jakarta.validation.constraints.*;

/**
 * DTO для создания новой квартиры
 */
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
    
    // Конструктор без параметров
    public CreateFlatRequest() {}
    
    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public CreateCoordinatesRequest getCoordinates() { return coordinates; }
    public void setCoordinates(CreateCoordinatesRequest coordinates) { this.coordinates = coordinates; }
    
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
    
    public Long getHouseId() { return houseId; }
    public void setHouseId(Long houseId) { this.houseId = houseId; }
}
