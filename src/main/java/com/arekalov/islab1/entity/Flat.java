package com.arekalov.islab1.entity;

// import jakarta.persistence.*;
// import jakarta.validation.constraints.*;
import java.time.ZonedDateTime;

/**
 * Основная сущность квартиры (POJO для нативного EclipseLink)
 */
// @Entity
// @Table(name = "flats")
public class Flat {
    
    // @Id
    // @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    // @NotBlank(message = "Название не может быть пустым")
    // @Column(name = "name", nullable = false)
    private String name;
    
    // @NotNull(message = "Координаты не могут быть null")
    // @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    // @JoinColumn(name = "coordinates_id", nullable = false)
    private Coordinates coordinates;
    
    // @Column(name = "creation_date", nullable = false)
    private ZonedDateTime creationDate;
    
    // @Min(value = 1, message = "Площадь должна быть больше 0")
    // @Column(name = "area", nullable = false)
    private Long area;
    
    // @Min(value = 1, message = "Цена должна быть больше 0")
    // @Max(value = 581208244, message = "Максимальная цена: 581208244")
    // @Column(name = "price", nullable = false)
    private Long price;
    
    // @Column(name = "balcony")
    private Boolean balcony;
    
    // @Min(value = 1, message = "Время до метро должно быть больше 0")
    // @Column(name = "time_to_metro_on_foot", nullable = false)
    private Long timeToMetroOnFoot;
    
    // @Min(value = 1, message = "Количество комнат должно быть больше 0")
    // @Max(value = 13, message = "Максимальное количество комнат: 13")
    // @Column(name = "number_of_rooms", nullable = false)
    private Integer numberOfRooms;
    
    // @Min(value = 1, message = "Жилая площадь должна быть больше 0")
    // @Column(name = "living_space", nullable = false)
    private Long livingSpace;
    
    // @NotNull(message = "Тип мебели не может быть null")
    // @Enumerated(EnumType.STRING)
    // @Column(name = "furnish", nullable = false)
    private Furnish furnish;
    
    // @NotNull(message = "Вид из окна не может быть null")
    // @Enumerated(EnumType.STRING)
    // @Column(name = "view", nullable = false)
    private View view;
    
    // @ManyToOne(fetch = FetchType.LAZY)
    // @JoinColumn(name = "house_id")
    private House house;
    
    // Конструктор без параметров
    public Flat() {
        this.creationDate = ZonedDateTime.now();
    }
    
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
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public Coordinates getCoordinates() {
        return coordinates;
    }
    
    public void setCoordinates(Coordinates coordinates) {
        this.coordinates = coordinates;
    }
    
    public ZonedDateTime getCreationDate() {
        return creationDate;
    }
    
    public void setCreationDate(ZonedDateTime creationDate) {
        this.creationDate = creationDate;
    }
    
    public Long getArea() {
        return area;
    }
    
    public void setArea(Long area) {
        this.area = area;
    }
    
    public Long getPrice() {
        return price;
    }
    
    public void setPrice(Long price) {
        this.price = price;
    }
    
    public Boolean getBalcony() {
        return balcony;
    }
    
    public void setBalcony(Boolean balcony) {
        this.balcony = balcony;
    }
    
    public Long getTimeToMetroOnFoot() {
        return timeToMetroOnFoot;
    }
    
    public void setTimeToMetroOnFoot(Long timeToMetroOnFoot) {
        this.timeToMetroOnFoot = timeToMetroOnFoot;
    }
    
    public Integer getNumberOfRooms() {
        return numberOfRooms;
    }
    
    public void setNumberOfRooms(Integer numberOfRooms) {
        this.numberOfRooms = numberOfRooms;
    }
    
    public Long getLivingSpace() {
        return livingSpace;
    }
    
    public void setLivingSpace(Long livingSpace) {
        this.livingSpace = livingSpace;
    }
    
    public Furnish getFurnish() {
        return furnish;
    }
    
    public void setFurnish(Furnish furnish) {
        this.furnish = furnish;
    }
    
    public View getView() {
        return view;
    }
    
    public void setView(View view) {
        this.view = view;
    }
    
    public House getHouse() {
        return house;
    }
    
    public void setHouse(House house) {
        this.house = house;
    }
    
    @Override
    public String toString() {
        return "Flat{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", coordinates=" + coordinates +
                ", creationDate=" + creationDate +
                ", area=" + area +
                ", price=" + price +
                ", balcony=" + balcony +
                ", timeToMetroOnFoot=" + timeToMetroOnFoot +
                ", numberOfRooms=" + numberOfRooms +
                ", livingSpace=" + livingSpace +
                ", furnish=" + furnish +
                ", view=" + view +
                ", house=" + house +
                '}';
    }
}
