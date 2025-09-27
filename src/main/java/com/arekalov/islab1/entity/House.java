package com.arekalov.islab1.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;

/**
 * Сущность дома
 */
@Entity
@Table(name = "houses")
public class House {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "name")
    private String name;
    
    @Min(value = 1, message = "Год постройки должен быть больше 0")
    @Column(name = "year", nullable = false)
    private Integer year;
    
    @Min(value = 1, message = "Количество квартир на этаже должно быть больше 0")
    @Column(name = "number_of_flats_on_floor", nullable = false)
    private Integer numberOfFlatsOnFloor;
    
    // Конструктор без параметров для JPA
    public House() {}
    
    public House(String name, Integer year, Integer numberOfFlatsOnFloor) {
        this.name = name;
        this.year = year;
        this.numberOfFlatsOnFloor = numberOfFlatsOnFloor;
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
    
    public Integer getYear() {
        return year;
    }
    
    public void setYear(Integer year) {
        this.year = year;
    }
    
    public Integer getNumberOfFlatsOnFloor() {
        return numberOfFlatsOnFloor;
    }
    
    public void setNumberOfFlatsOnFloor(Integer numberOfFlatsOnFloor) {
        this.numberOfFlatsOnFloor = numberOfFlatsOnFloor;
    }
    
    @Override
    public String toString() {
        return "House{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", year=" + year +
                ", numberOfFlatsOnFloor=" + numberOfFlatsOnFloor +
                '}';
    }
}
