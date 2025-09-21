package com.arekalov.islab1.entity

import jakarta.persistence.*
import jakarta.validation.constraints.*
import java.time.ZonedDateTime

/**
 * Основная сущность квартиры
 */
@Entity
@Table(name = "flats")
data class Flat(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    
    @field:NotBlank(message = "Название не может быть пустым")
    @Column(name = "name", nullable = false)
    val name: String,
    
    @field:NotNull(message = "Координаты не могут быть null")
    @OneToOne(cascade = [CascadeType.ALL], fetch = FetchType.EAGER)
    @JoinColumn(name = "coordinates_id", nullable = false)
    val coordinates: Coordinates,
    
    @Column(name = "creation_date", nullable = false)
    val creationDate: ZonedDateTime = ZonedDateTime.now(),
    
    @field:Min(value = 1, message = "Площадь должна быть больше 0")
    @Column(name = "area", nullable = false)
    val area: Long,
    
    @field:Min(value = 1, message = "Цена должна быть больше 0")
    @field:Max(value = 581208244, message = "Максимальная цена: 581208244")
    @Column(name = "price", nullable = false)
    val price: Long,
    
    @Column(name = "balcony")
    val balcony: Boolean? = null,
    
    @field:Min(value = 1, message = "Время до метро должно быть больше 0")
    @Column(name = "time_to_metro_on_foot", nullable = false)
    val timeToMetroOnFoot: Long,
    
    @field:Min(value = 1, message = "Количество комнат должно быть больше 0")
    @field:Max(value = 13, message = "Максимальное количество комнат: 13")
    @Column(name = "number_of_rooms", nullable = false)
    val numberOfRooms: Int,
    
    @field:Min(value = 1, message = "Жилая площадь должна быть больше 0")
    @Column(name = "living_space", nullable = false)
    val livingSpace: Long,
    
    @field:NotNull(message = "Тип мебели не может быть null")
    @Enumerated(EnumType.STRING)
    @Column(name = "furnish", nullable = false)
    val furnish: Furnish,
    
    @field:NotNull(message = "Вид из окна не может быть null")
    @Enumerated(EnumType.STRING)
    @Column(name = "view", nullable = false)
    val view: View,
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "house_id")
    val house: House? = null
) {
    // Конструктор без параметров для JPA
    constructor() : this(
        id = null,
        name = "",
        coordinates = Coordinates(),
        creationDate = ZonedDateTime.now(),
        area = 1,
        price = 1,
        balcony = null,
        timeToMetroOnFoot = 1,
        numberOfRooms = 1,
        livingSpace = 1,
        furnish = Furnish.NONE,
        view = View.STREET,
        house = null
    )
}
