package com.arekalov.islab1.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

/**
 * Сущность координат
 */
@Entity
@Table(name = "coordinates")
public class Coordinates {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotNull(message = "Координата X не может быть null")
    @Column(name = "x", nullable = false)
    private Integer x;
    
    @Column(name = "y", nullable = false)
    private Integer y;
    
    // Конструктор без параметров для JPA
    public Coordinates() {}
    
    public Coordinates(Integer x, Integer y) {
        this.x = x;
        this.y = y;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Integer getX() {
        return x;
    }
    
    public void setX(Integer x) {
        this.x = x;
    }
    
    public Integer getY() {
        return y;
    }
    
    public void setY(Integer y) {
        this.y = y;
    }
    
    @Override
    public String toString() {
        return "Coordinates{" +
                "id=" + id +
                ", x=" + x +
                ", y=" + y +
                '}';
    }
}
