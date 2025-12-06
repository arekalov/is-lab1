package com.arekalov.islab1.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Координаты (Entity)
 */
@Entity
@Table(name = "coordinates")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Coordinates {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Координата X не может быть null")
    @Column(nullable = false)
    private Integer x;

    @NotNull(message = "Координата Y не может быть null")
    @Min(value = -515, message = "Координата Y должна быть больше -515")
    @Column(nullable = false)
    private Integer y;
}