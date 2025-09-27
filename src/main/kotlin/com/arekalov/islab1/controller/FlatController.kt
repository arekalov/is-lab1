package com.arekalov.islab1.controller

import com.arekalov.islab1.dto.*
import com.arekalov.islab1.service.FlatService
import jakarta.inject.Inject
import jakarta.validation.Valid
import jakarta.ws.rs.*
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response

/**
 * REST контроллер для работы с квартирами
 */
@Path("/flats")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
open class FlatController {
    
    @Inject
    private lateinit var flatService: FlatService
    
    /**
     * Получить список квартир с пагинацией и фильтрацией
     */
    @GET
    open fun getFlats(
        @QueryParam("page") @DefaultValue("0") page: Int,
        @QueryParam("size") @DefaultValue("20") size: Int,
        @QueryParam("sort") @DefaultValue("id") sortBy: String,
        @QueryParam("name") nameFilter: String?,
        @QueryParam("minPrice") minPrice: Long?,
        @QueryParam("maxPrice") maxPrice: Long?,
        @QueryParam("hasBalcony") hasBalcony: Boolean?,
        @QueryParam("minRooms") minRooms: Int?,
        @QueryParam("maxRooms") maxRooms: Int?
    ): Response {
        return try {
            val result = flatService.getAllFlats(
                page = page,
                size = size,
                sortBy = sortBy,
                nameFilter = nameFilter,
                minPrice = minPrice,
                maxPrice = maxPrice,
                hasBalcony = hasBalcony,
                minRooms = minRooms,
                maxRooms = maxRooms
            )
            Response.ok(result).build()
        } catch (e: Exception) {
            Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(ErrorResponse("Ошибка получения списка квартир: ${e.message}"))
                .build()
        }
    }
    
    /**
     * Получить квартиру по ID
     */
    @GET
    @Path("/{id}")
    open fun getFlatById(@PathParam("id") id: Long): Response {
        return try {
            val flat = flatService.getFlatById(id)
            if (flat != null) {
                Response.ok(flat).build()
            } else {
                Response.status(Response.Status.NOT_FOUND)
                    .entity(ErrorResponse("Квартира с ID $id не найдена"))
                    .build()
            }
        } catch (e: Exception) {
            Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(ErrorResponse("Ошибка получения квартиры: ${e.message}"))
                .build()
        }
    }
    
    /**
     * Создать новую квартиру
     */
    @POST
    open fun createFlat(@Valid request: CreateFlatRequest): Response {
        return try {
            val createdFlat = flatService.createFlat(request)
            Response.status(Response.Status.CREATED)
                .entity(createdFlat)
                .build()
        } catch (e: Exception) {
            Response.status(Response.Status.BAD_REQUEST)
                .entity(ErrorResponse("Ошибка создания квартиры: ${e.message}"))
                .build()
        }
    }
    
    /**
     * Обновить квартиру
     */
    @PUT
    @Path("/{id}")
    open fun updateFlat(@PathParam("id") id: Long, @Valid request: CreateFlatRequest): Response {
        return try {
            val updatedFlat = flatService.updateFlat(id, request)
            if (updatedFlat != null) {
                Response.ok(updatedFlat).build()
            } else {
                Response.status(Response.Status.NOT_FOUND)
                    .entity(ErrorResponse("Квартира с ID $id не найдена"))
                    .build()
            }
        } catch (e: Exception) {
            Response.status(Response.Status.BAD_REQUEST)
                .entity(ErrorResponse("Ошибка обновления квартиры: ${e.message}"))
                .build()
        }
    }
    
    /**
     * Удалить квартиру
     */
    @DELETE
    @Path("/{id}")
    open fun deleteFlat(@PathParam("id") id: Long): Response {
        return try {
            val deleted = flatService.deleteFlat(id)
            if (deleted) {
                Response.noContent().build()
            } else {
                Response.status(Response.Status.NOT_FOUND)
                    .entity(ErrorResponse("Квартира с ID $id не найдена"))
                    .build()
            }
        } catch (e: Exception) {
            Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(ErrorResponse("Ошибка удаления квартиры: ${e.message}"))
                .build()
        }
    }
    
    /**
     * Специальные операции
     */
    
    /**
     * Количество квартир с комнатами больше заданного
     */
    @GET
    @Path("/count-by-rooms")
    open fun countByRoomsGreaterThan(@QueryParam("minRooms") minRooms: Int): Response {
        return try {
            val count = flatService.countByRoomsGreaterThan(minRooms)
            Response.ok(mapOf("count" to count)).build()
        } catch (e: Exception) {
            Response.status(Response.Status.BAD_REQUEST)
                .entity(ErrorResponse("Ошибка подсчета квартир: ${e.message}"))
                .build()
        }
    }
    
    /**
     * Квартиры с названием содержащим подстроку
     */
    @GET
    @Path("/by-name")
    open fun findByNameContaining(@QueryParam("substring") substring: String): Response {
        return try {
            val flats = flatService.findByNameContaining(substring)
            Response.ok(flats).build()
        } catch (e: Exception) {
            Response.status(Response.Status.BAD_REQUEST)
                .entity(ErrorResponse("Ошибка поиска квартир: ${e.message}"))
                .build()
        }
    }
    
    /**
     * Квартиры с жилой площадью меньше заданной
     */
    @GET
    @Path("/by-living-space")
    open fun findByLivingSpaceLessThan(@QueryParam("maxSpace") maxSpace: Long): Response {
        return try {
            val flats = flatService.findByLivingSpaceLessThan(maxSpace)
            Response.ok(flats).build()
        } catch (e: Exception) {
            Response.status(Response.Status.BAD_REQUEST)
                .entity(ErrorResponse("Ошибка поиска квартир: ${e.message}"))
                .build()
        }
    }
    
    /**
     * Самая дешевая квартира с балконом
     */
    @GET
    @Path("/cheapest-with-balcony")
    open fun findCheapestWithBalcony(): Response {
        return try {
            val flat = flatService.findCheapestWithBalcony()
            if (flat != null) {
                Response.ok(flat).build()
            } else {
                Response.status(Response.Status.NOT_FOUND)
                    .entity(ErrorResponse("Квартира с балконом не найдена"))
                    .build()
            }
        } catch (e: Exception) {
            Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(ErrorResponse("Ошибка поиска квартиры: ${e.message}"))
                .build()
        }
    }
    
    /**
     * Квартиры отсортированные по времени до метро
     */
    @GET
    @Path("/sorted-by-metro-time")
    open fun findAllSortedByMetroTime(): Response {
        return try {
            val flats = flatService.findAllSortedByMetroTime()
            Response.ok(flats).build()
        } catch (e: Exception) {
            Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(ErrorResponse("Ошибка получения квартир: ${e.message}"))
                .build()
        }
    }
}
