package com.arekalov.islab1.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import lombok.*;

/**
 * Сущность дома
 */
@Data
@NoArgsConstructor
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

    public House(String name, Integer year, Integer numberOfFlatsOnFloor) {
        this.name = name;
        this.year = year;
        this.numberOfFlatsOnFloor = numberOfFlatsOnFloor;
    }
}
