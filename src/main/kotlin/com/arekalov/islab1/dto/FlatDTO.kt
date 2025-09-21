package com.arekalov.islab1.dto

import com.arekalov.islab1.entity.Furnish
import com.arekalov.islab1.entity.View
import com.fasterxml.jackson.annotation.JsonFormat
import java.time.ZonedDateTime

/**
 * DTO для передачи данных о квартире
 */
data class FlatDTO(
    val id: Long?,
    val name: String,
    val coordinates: CoordinatesDTO,
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    val creationDate: ZonedDateTime,
    val area: Long,
    val price: Long,
    val balcony: Boolean?,
    val timeToMetroOnFoot: Long,
    val numberOfRooms: Int,
    val livingSpace: Long,
    val furnish: Furnish,
    val view: View,
    val house: HouseDTO?
)

/**
 * DTO для координат
 */
data class CoordinatesDTO(
    val id: Long?,
    val x: Int,
    val y: Int
)

/**
 * DTO для дома
 */
data class HouseDTO(
    val id: Long?,
    val name: String?,
    val year: Int,
    val numberOfFlatsOnFloor: Int
)
