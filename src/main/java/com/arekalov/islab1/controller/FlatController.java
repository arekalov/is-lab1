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
 * REST контроллер для работы с квартирами на нативном EclipseLink
 */
@Path("/flats")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class FlatController {
    
    @Inject
    private FlatService flatService;
    
    /**
     * Получить список всех квартир
     */
    @GET
    public Response getFlats() {
        try {
            // Получаем все квартиры без пагинации
            List<FlatDTO> flats = flatService.getAllFlats()
                .stream()
                .map(this::convertToDTO)
                .toList();
            
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
            com.arekalov.islab1.entity.Flat flat = flatService.getFlatById(id);
            if (flat != null) {
                FlatDTO flatDTO = convertToDTO(flat);
                return Response.ok(flatDTO).build();
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
            com.arekalov.islab1.entity.Flat flat = convertFromRequest(request);
            com.arekalov.islab1.entity.Flat createdFlat = flatService.createFlat(flat);
            FlatDTO flatDTO = convertToDTO(createdFlat);
            return Response.status(Response.Status.CREATED).entity(flatDTO).build();
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
            com.arekalov.islab1.entity.Flat updatedFlat = convertFromRequest(request);
            com.arekalov.islab1.entity.Flat result = flatService.updateFlat(id, updatedFlat);
            FlatDTO flatDTO = convertToDTO(result);
            return Response.ok(flatDTO).build();
        } catch (RuntimeException e) {
            if (e.getMessage().contains("не найдена")) {
                return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorResponse(e.getMessage()))
                    .build();
            } else {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("Ошибка обновления квартиры: " + e.getMessage()))
                    .build();
            }
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
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
    
    /**
     * Конвертировать Entity в DTO
     */
    private FlatDTO convertToDTO(com.arekalov.islab1.entity.Flat flat) {
        FlatDTO dto = new FlatDTO();
        dto.setId(flat.getId());
        dto.setName(flat.getName());
        dto.setArea(flat.getArea());
        dto.setPrice(flat.getPrice());
        dto.setBalcony(flat.getBalcony());
        dto.setTimeToMetroOnFoot(flat.getTimeToMetroOnFoot());
        dto.setNumberOfRooms(flat.getNumberOfRooms());
        dto.setLivingSpace(flat.getLivingSpace());
        dto.setFurnish(flat.getFurnish());
        dto.setView(flat.getView());
        dto.setCreationDate(flat.getCreationDate());
        
        // Конвертируем координаты
        if (flat.getCoordinates() != null) {
            CoordinatesDTO coordsDTO = new CoordinatesDTO();
            coordsDTO.setId(flat.getCoordinates().getId());
            coordsDTO.setX(flat.getCoordinates().getX());
            coordsDTO.setY(flat.getCoordinates().getY());
            dto.setCoordinates(coordsDTO);
        }
        
        // Конвертируем дом
        if (flat.getHouse() != null) {
            HouseDTO houseDTO = new HouseDTO();
            houseDTO.setId(flat.getHouse().getId());
            houseDTO.setName(flat.getHouse().getName());
            houseDTO.setYear(flat.getHouse().getYear());
            houseDTO.setNumberOfFlatsOnFloor(flat.getHouse().getNumberOfFlatsOnFloor());
            dto.setHouse(houseDTO);
        }
        
        return dto;
    }
    
    /**
     * Конвертировать Request в Entity
     */
    private com.arekalov.islab1.entity.Flat convertFromRequest(CreateFlatRequest request) {
        com.arekalov.islab1.entity.Flat flat = new com.arekalov.islab1.entity.Flat();
        flat.setName(request.getName());
        flat.setArea(request.getArea());
        flat.setPrice(request.getPrice());
        flat.setBalcony(request.getBalcony());
        flat.setTimeToMetroOnFoot(request.getTimeToMetroOnFoot());
        flat.setNumberOfRooms(request.getNumberOfRooms());
        flat.setLivingSpace(request.getLivingSpace());
        flat.setFurnish(request.getFurnish());
        flat.setView(request.getView());
        
        // Конвертируем координаты
        if (request.getCoordinates() != null) {
            com.arekalov.islab1.entity.Coordinates coords = new com.arekalov.islab1.entity.Coordinates();
            coords.setX(request.getCoordinates().getX());
            coords.setY(request.getCoordinates().getY());
            flat.setCoordinates(coords);
        }
        
        // Конвертируем дом (если указан ID)
        if (request.getHouseId() != null) {
            com.arekalov.islab1.entity.House house = new com.arekalov.islab1.entity.House();
            house.setId(request.getHouseId());
            flat.setHouse(house);
        }
        
        return flat;
    }
}
