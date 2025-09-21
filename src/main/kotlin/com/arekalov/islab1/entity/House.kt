package com.arekalov.islab1.entity

import jakarta.persistence.*
import jakarta.validation.constraints.Min

/**
 * Сущность дома
 */
@Entity
@Table(name = "houses")
data class House(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    
    @Column(name = "name")
    val name: String? = null,
    
    @field:Min(value = 1, message = "Год постройки должен быть больше 0")
    @Column(name = "year", nullable = false)
    val year: Int,
    
    @field:Min(value = 1, message = "Количество квартир на этаже должно быть больше 0")
    @Column(name = "number_of_flats_on_floor", nullable = false)
    val numberOfFlatsOnFloor: Int
) {
    // Конструктор без параметров для JPA
    constructor() : this(null, null, 1, 1)
}
