package com.arekalov.islab1.dto

import com.arekalov.islab1.entity.Furnish
import com.arekalov.islab1.entity.View
import jakarta.validation.constraints.*

/**
 * DTO для создания новой квартиры
 */
data class CreateFlatRequest(
    @field:NotBlank(message = "Название не может быть пустым")
    val name: String,
    
    @field:NotNull(message = "Координаты не могут быть null")
    val coordinates: CreateCoordinatesRequest,
    
    @field:Min(value = 1, message = "Площадь должна быть больше 0")
    val area: Long,
    
    @field:Min(value = 1, message = "Цена должна быть больше 0")
    @field:Max(value = 581208244, message = "Максимальная цена: 581208244")
    val price: Long,
    
    val balcony: Boolean?,
    
    @field:Min(value = 1, message = "Время до метро должно быть больше 0")
    val timeToMetroOnFoot: Long,
    
    @field:Min(value = 1, message = "Количество комнат должно быть больше 0")
    @field:Max(value = 13, message = "Максимальное количество комнат: 13")
    val numberOfRooms: Int,
    
    @field:Min(value = 1, message = "Жилая площадь должна быть больше 0")
    val livingSpace: Long,
    
    @field:NotNull(message = "Тип мебели не может быть null")
    val furnish: Furnish,
    
    @field:NotNull(message = "Вид из окна не может быть null")
    val view: View,
    
    val houseId: Long?
)

/**
 * DTO для создания координат
 */
data class CreateCoordinatesRequest(
    @field:NotNull(message = "Координата X не может быть null")
    val x: Int,
    
    val y: Int
)

/**
 * DTO для создания дома
 */
data class CreateHouseRequest(
    val name: String?,
    
    @field:Min(value = 1, message = "Год постройки должен быть больше 0")
    val year: Int,
    
    @field:Min(value = 1, message = "Количество квартир на этаже должно быть больше 0")
    val numberOfFlatsOnFloor: Int
)
