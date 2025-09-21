package com.arekalov.islab1.controller

import com.arekalov.islab1.dto.CreateHouseRequest
import com.arekalov.islab1.dto.ErrorResponse
import com.arekalov.islab1.service.HouseService
import jakarta.inject.Inject
import jakarta.validation.Valid
import jakarta.ws.rs.*
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response

/**
 * REST контроллер для работы с домами
 */
@Path("/houses")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
class HouseController @Inject constructor(
    private val houseService: HouseService
) {
    
    /**
     * Получить список всех домов
     */
    @GET
    fun getAllHouses(): Response {
        return try {
            val houses = houseService.getAllHouses()
            Response.ok(houses).build()
        } catch (e: Exception) {
            Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(ErrorResponse("Ошибка получения списка домов: ${e.message}"))
                .build()
        }
    }
    
    /**
     * Получить дом по ID
     */
    @GET
    @Path("/{id}")
    fun getHouseById(@PathParam("id") id: Long): Response {
        return try {
            val house = houseService.getHouseById(id)
            if (house != null) {
                Response.ok(house).build()
            } else {
                Response.status(Response.Status.NOT_FOUND)
                    .entity(ErrorResponse("Дом с ID $id не найден"))
                    .build()
            }
        } catch (e: Exception) {
            Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(ErrorResponse("Ошибка получения дома: ${e.message}"))
                .build()
        }
    }
    
    /**
     * Создать новый дом
     */
    @POST
    fun createHouse(@Valid request: CreateHouseRequest): Response {
        return try {
            val createdHouse = houseService.createHouse(request)
            Response.status(Response.Status.CREATED)
                .entity(createdHouse)
                .build()
        } catch (e: Exception) {
            Response.status(Response.Status.BAD_REQUEST)
                .entity(ErrorResponse("Ошибка создания дома: ${e.message}"))
                .build()
        }
    }
    
    /**
     * Обновить дом
     */
    @PUT
    @Path("/{id}")
    fun updateHouse(@PathParam("id") id: Long, @Valid request: CreateHouseRequest): Response {
        return try {
            val updatedHouse = houseService.updateHouse(id, request)
            if (updatedHouse != null) {
                Response.ok(updatedHouse).build()
            } else {
                Response.status(Response.Status.NOT_FOUND)
                    .entity(ErrorResponse("Дом с ID $id не найден"))
                    .build()
            }
        } catch (e: Exception) {
            Response.status(Response.Status.BAD_REQUEST)
                .entity(ErrorResponse("Ошибка обновления дома: ${e.message}"))
                .build()
        }
    }
    
    /**
     * Удалить дом
     */
    @DELETE
    @Path("/{id}")
    fun deleteHouse(@PathParam("id") id: Long): Response {
        return try {
            val deleted = houseService.deleteHouse(id)
            if (deleted) {
                Response.noContent().build()
            } else {
                Response.status(Response.Status.NOT_FOUND)
                    .entity(ErrorResponse("Дом с ID $id не найден"))
                    .build()
            }
        } catch (e: Exception) {
            Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(ErrorResponse("Ошибка удаления дома: ${e.message}"))
                .build()
        }
    }
    
    /**
     * Поиск домов по названию
     */
    @GET
    @Path("/search")
    fun findByNameContaining(@QueryParam("name") substring: String): Response {
        return try {
            val houses = houseService.findByNameContaining(substring)
            Response.ok(houses).build()
        } catch (e: Exception) {
            Response.status(Response.Status.BAD_REQUEST)
                .entity(ErrorResponse("Ошибка поиска домов: ${e.message}"))
                .build()
        }
    }
}
