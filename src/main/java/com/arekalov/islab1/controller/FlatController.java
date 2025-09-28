package com.arekalov.islab1.controller;

import com.arekalov.islab1.dto.*;
import com.arekalov.islab1.service.FlatService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;

/**
 * REST контроллер для работы с квартирами
 */
@Path("/flats")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class FlatController {
    
    @Inject
    private FlatService flatService;
    
    /**
     * Получить список квартир с пагинацией и фильтрацией
     */
    @GET
    public Response getFlats(@QueryParam("page") @DefaultValue("0") int page,
                            @QueryParam("size") @DefaultValue("20") int size,
                            @QueryParam("sortBy") @DefaultValue("id") String sortBy,
                            @QueryParam("name") String nameFilter,
                            @QueryParam("minPrice") Long minPrice,
                            @QueryParam("maxPrice") Long maxPrice,
                            @QueryParam("hasBalcony") Boolean hasBalcony,
                            @QueryParam("minRooms") Integer minRooms,
                            @QueryParam("maxRooms") Integer maxRooms) {
        try {
            PagedResponse<FlatDTO> flats = flatService.getAllFlats(
                page, size, sortBy, nameFilter, minPrice, maxPrice,
                hasBalcony, minRooms, maxRooms
            );
            return Response.ok(flats).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ErrorResponse("Ошибка получения списка квартир: " + e.getMessage()))
                .build();
        }
    }
    
    /**
     * Получить квартиру по ID
     */
    @GET
    @Path("/{id}")
    public Response getFlatById(@PathParam("id") Long id) {
        try {
            FlatDTO flat = flatService.getFlatById(id);
            if (flat != null) {
                return Response.ok(flat).build();
            } else {
                return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorResponse("Квартира с ID " + id + " не найдена"))
                    .build();
            }
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ErrorResponse("Ошибка получения квартиры: " + e.getMessage()))
                .build();
        }
    }
    
    /**
     * Создать новую квартиру
     */
    @POST
    public Response createFlat(@Valid CreateFlatRequest request) {
        try {
            FlatDTO flat = flatService.createFlat(request);
            return Response.status(Response.Status.CREATED).entity(flat).build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ErrorResponse("Ошибка создания квартиры: " + e.getMessage()))
                .build();
        }
    }
    
    /**
     * Обновить квартиру
     */
    @PUT
    @Path("/{id}")
    public Response updateFlat(@PathParam("id") Long id, @Valid CreateFlatRequest request) {
        try {
            FlatDTO flat = flatService.updateFlat(id, request);
            if (flat != null) {
                return Response.ok(flat).build();
            } else {
                return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorResponse("Квартира с ID " + id + " не найдена"))
                    .build();
            }
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ErrorResponse("Ошибка обновления квартиры: " + e.getMessage()))
                .build();
        }
    }
    
    /**
     * Удалить квартиру
     */
    @DELETE
    @Path("/{id}")
    public Response deleteFlat(@PathParam("id") Long id) {
        try {
            boolean deleted = flatService.deleteFlat(id);
            if (deleted) {
                return Response.noContent().build();
            } else {
                return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorResponse("Квартира с ID " + id + " не найдена"))
                    .build();
            }
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ErrorResponse("Ошибка удаления квартиры: " + e.getMessage()))
                .build();
        }
    }
    
    // === СПЕЦИАЛЬНЫЕ ОПЕРАЦИИ ===
    
    /**
     * Подсчитать квартиры с количеством комнат больше заданного
     */
    @GET
    @Path("/count-by-rooms")
    public Response countByRoomsGreaterThan(@QueryParam("minRooms") Integer minRooms) {
        try {
            if (minRooms == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("Параметр 'minRooms' обязателен"))
                    .build();
            }
            
            Long count = flatService.countByRoomsGreaterThan(minRooms);
            return Response.ok(count).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ErrorResponse("Ошибка подсчета квартир: " + e.getMessage()))
                .build();
        }
    }
    
    /**
     * Найти квартиры по названию (содержит подстроку)
     */
    @GET
    @Path("/search-by-name")
    public Response findByNameContaining(@QueryParam("name") String name) {
        try {
            if (name == null || name.trim().isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("Параметр 'name' не может быть пустым"))
                    .build();
            }
            
            List<FlatDTO> flats = flatService.findByNameContaining(name);
            return Response.ok(flats).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ErrorResponse("Ошибка поиска квартир: " + e.getMessage()))
                .build();
        }
    }
    
    /**
     * Найти квартиры с жилой площадью меньше заданной
     */
    @GET
    @Path("/by-living-space")
    public Response findByLivingSpaceLessThan(@QueryParam("maxSpace") Long maxSpace) {
        try {
            if (maxSpace == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("Параметр 'maxSpace' обязателен"))
                    .build();
            }
            
            List<FlatDTO> flats = flatService.findByLivingSpaceLessThan(maxSpace);
            return Response.ok(flats).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ErrorResponse("Ошибка поиска квартир: " + e.getMessage()))
                .build();
        }
    }
    
    /**
     * Найти самую дешевую квартиру с балконом
     */
    @GET
    @Path("/cheapest-with-balcony")
    public Response findCheapestWithBalcony() {
        try {
            FlatDTO flat = flatService.findCheapestWithBalcony();
            if (flat != null) {
                return Response.ok(flat).build();
            } else {
                return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorResponse("Квартиры с балконом не найдены"))
                    .build();
            }
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ErrorResponse("Ошибка поиска квартиры: " + e.getMessage()))
                .build();
        }
    }
    
    /**
     * Получить все квартиры, отсортированные по времени до метро
     */
    @GET
    @Path("/sorted-by-metro-time")
    public Response findAllSortedByMetroTime() {
        try {
            List<FlatDTO> flats = flatService.findAllSortedByMetroTime();
            return Response.ok(flats).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ErrorResponse("Ошибка получения квартир: " + e.getMessage()))
                .build();
        }
    }
}

