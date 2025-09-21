package com.arekalov.islab1.entity

import jakarta.persistence.*
import jakarta.validation.constraints.NotNull

/**
 * Сущность координат
 */
@Entity
@Table(name = "coordinates")
data class Coordinates(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    
    @field:NotNull(message = "Координата X не может быть null")
    @Column(name = "x", nullable = false)
    val x: Int,
    
    @Column(name = "y", nullable = false)
    val y: Int
) {
    // Конструктор без параметров для JPA
    constructor() : this(null, 0, 0)
}
