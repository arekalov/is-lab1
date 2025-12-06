package com.arekalov.islab1.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Дом (Entity)
 */
@Entity
@Table(name = "houses")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class House {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "name")
    private String name;
    
    @NotNull(message = "Год постройки не может быть null")
    @Positive(message = "Год постройки должен быть положительным")
    @Column(nullable = false)
    private Integer year;
    
    @NotNull(message = "Количество квартир на этаже не может быть null")
    @Positive(message = "Количество квартир на этаже должно быть положительным")
    @Column(name = "number_of_flats_on_floor", nullable = false)
    private Integer numberOfFlatsOnFloor;
}
