package com.arekalov.islab1.controller;

import com.arekalov.islab1.dto.request.CreateHouseRequest;
import com.arekalov.islab1.dto.response.ErrorResponse;
import com.arekalov.islab1.dto.response.PagedResponse;
import com.arekalov.islab1.dto.HouseDTO;
import com.arekalov.islab1.service.HouseService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;

/**
 * REST контроллер для работы с домами
 */
@Path("/houses")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class HouseController {
    
    @Inject
    private HouseService houseService;
    
    /**
     * Получить список всех домов с пагинацией
     */
    @GET
    public Response getAllHouses(@QueryParam("page") @DefaultValue("0") int page,
                                @QueryParam("size") @DefaultValue("10") int size) {
        try {
            // Валидация параметров пагинации
            if (page < 0) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("Номер страницы не может быть отрицательным"))
                    .build();
            }
            if (size <= 0 || size > 100) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("Размер страницы должен быть от 1 до 100"))
                    .build();
            }
            
            // Получаем данные с пагинацией
            List<HouseDTO> houses = houseService.getAllHouses(page, size);
            long total = houseService.countHouses();
            
            // Создаем пагинированный ответ
            PagedResponse<HouseDTO> pagedResponse = new PagedResponse<>(houses, total, page, size);
            
            return Response.ok(pagedResponse).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ErrorResponse("Ошибка получения списка домов: " + e.getMessage()))
                .build();
        }
    }
    
    /**
     * Получить дом по ID
     */
    @GET
    @Path("/{id}")
    public Response getHouseById(@PathParam("id") Long id) {
        try {
            HouseDTO house = houseService.getHouseById(id);
            if (house != null) {
                return Response.ok(house).build();
            } else {
                return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorResponse("Дом с ID " + id + " не найден"))
                    .build();
            }
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ErrorResponse("Ошибка получения дома: " + e.getMessage()))
                .build();
        }
    }
    
    /**
     * Создать новый дом
     */
    @POST
    public Response createHouse(@Valid CreateHouseRequest request) {
        try {
            HouseDTO house = houseService.createHouse(request);
            return Response.status(Response.Status.CREATED).entity(house).build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ErrorResponse("Ошибка создания дома: " + e.getMessage()))
                .build();
        }
    }
    
    /**
     * Обновить дом
     */
    @PUT
    @Path("/{id}")
    public Response updateHouse(@PathParam("id") Long id, @Valid CreateHouseRequest request) {
        try {
            HouseDTO house = houseService.updateHouse(id, request);
            if (house != null) {
                return Response.ok(house).build();
            } else {
                return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorResponse("Дом с ID " + id + " не найден"))
                    .build();
            }
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ErrorResponse("Ошибка обновления дома: " + e.getMessage()))
                .build();
        }
    }
    
    /**
     * Удалить дом
     */
    @DELETE
    @Path("/{id}")
    public Response deleteHouse(@PathParam("id") Long id) {
        try {
            boolean deleted = houseService.deleteHouse(id);
            if (deleted) {
                return Response.noContent().build();
            } else {
                return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorResponse("Дом с ID " + id + " не найден"))
                    .build();
            }
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ErrorResponse("Ошибка удаления дома: " + e.getMessage()))
                .build();
        }
    }
    
    /**
     * Поиск домов по названию
     */
    @GET
    @Path("/search")
    public Response searchHouses(@QueryParam("name") String name) {
        try {
            if (name == null || name.trim().isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("Параметр 'name' не может быть пустым"))
                    .build();
            }
            
            List<HouseDTO> houses = houseService.findByNameContaining(name);
            return Response.ok(houses).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ErrorResponse("Ошибка поиска домов: " + e.getMessage()))
                .build();
        }
    }
}
