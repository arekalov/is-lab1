package com.arekalov.islab1.controller;

import com.arekalov.islab1.dto.*;
import com.arekalov.islab1.dto.request.CreateFlatRequest;
import com.arekalov.islab1.dto.response.ErrorResponse;
import com.arekalov.islab1.dto.response.PagedResponse;
import com.arekalov.islab1.service.FlatService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.logging.Logger;

/**
 * REST контроллер для работы с квартирами на нативном EclipseLink
 */
@Path("/flats")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class FlatController {
    
    private static final Logger logger = Logger.getLogger(FlatController.class.getName());
    
    @Inject
    private FlatService flatService;
    
    /**
     * Получить список всех квартир с пагинацией
     */
    @GET
    public Response getFlats(@QueryParam("page") @DefaultValue("0") int page,
                            @QueryParam("size") @DefaultValue("10") int size,
                            @QueryParam("sortBy") @DefaultValue("id") String sortBy) {
        try {
            // Расширенная валидация параметров пагинации
            if (page < 0) {
                logger.warning("FlatController.getFlats() - Некорректный page: " + page);
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("Номер страницы не может быть отрицательным"))
                    .build();
            }
            if (size <= 0) {
                logger.warning("FlatController.getFlats() - Некорректный size: " + size);
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("Размер страницы должен быть больше 0"))
                    .build();
            }
            if (size > 100) {
                logger.warning("FlatController.getFlats() - Слишком большой size: " + size);
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("Размер страницы не может быть больше 100"))
                    .build();
            }
            
            // Валидация параметра сортировки
            if (!isValidSortBy(sortBy)) {
                logger.warning("FlatController.getFlats() - Некорректный sortBy: " + sortBy);
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("Недопустимое поле для сортировки: " + sortBy + 
                        ". Доступные поля: id, name, price, area"))
                    .build();
            }
            
            logger.info("FlatController.getFlats() - Запрос пагинации: page=" + page + ", size=" + size + ", sortBy=" + sortBy);
            
            // Получаем данные с пагинацией
            List<com.arekalov.islab1.pojo.Flat> flats = flatService.getAllFlats(page, size, sortBy);
            long total = flatService.countFlats();
            
            logger.info("FlatController.getFlats() - Получено квартир: " + flats.size() + ", общее количество: " + total);
            
            // Конвертируем в DTO
            List<FlatDTO> flatDTOs = flats.stream()
                .map(this::convertToDTO)
                .toList();
            
            // Создаем пагинированный ответ
            PagedResponse<FlatDTO> pagedResponse = new PagedResponse<>(flatDTOs, total, page, size);
            
            return Response.ok(pagedResponse).build();
        } catch (Exception e) {
            logger.severe("FlatController.getFlats() - Ошибка: " + e.getMessage());
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ErrorResponse("Ошибка получения списка квартир: " + e.getMessage()))
                .build();
        }
    }
    
    /**
     * Проверка валидности поля для сортировки
     */
    private boolean isValidSortBy(String sortBy) {
        return "id".equals(sortBy) || "name".equals(sortBy) || 
               "price".equals(sortBy) || "area".equals(sortBy);
    }
    
    /**
     * Получить квартиру по ID
     */
    @GET
    @Path("/{id}")
    public Response getFlatById(@PathParam("id") Long id) {
        try {
            com.arekalov.islab1.pojo.Flat flat = flatService.getFlatById(id);
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
            com.arekalov.islab1.pojo.Flat flat = convertFromRequest(request);
            com.arekalov.islab1.pojo.Flat createdFlat = flatService.createFlat(flat);
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
            com.arekalov.islab1.pojo.Flat updatedFlat = convertFromRequest(request);
            com.arekalov.islab1.pojo.Flat result = flatService.updateFlat(id, updatedFlat);
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
     * Подсчитать количество квартир с количеством комнат больше заданного
     */
    @GET
    @Path("/count/rooms-greater-than/{minRooms}")
    public Response countByRoomsGreaterThan(@PathParam("minRooms") Integer minRooms) {
        try {
            if (minRooms == null || minRooms < 0) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("Количество комнат должно быть неотрицательным числом"))
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
     * Найти квартиры, содержащие подстроку в названии
     */
    @GET
    @Path("/search/by-name")
    public Response findByNameContaining(@QueryParam("substring") String nameSubstring) {
        try {
            // Проверка входных данных
            if (nameSubstring == null || nameSubstring.trim().isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("Подстрока для поиска не может быть пустой"))
                    .build();
            }
            
            // Ограничение длины поискового запроса
            String trimmedSubstring = nameSubstring.trim();
            if (trimmedSubstring.length() > 100) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("Длина поисковой строки не может превышать 100 символов"))
                    .build();
            }
            
            // Проверка на специальные символы
            if (!trimmedSubstring.matches("^[\\p{L}\\p{N}\\s\\-_.,]+$")) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("Поисковая строка содержит недопустимые символы"))
                    .build();
            }
            
            List<com.arekalov.islab1.pojo.Flat> flats = flatService.findByNameContaining(trimmedSubstring);
            List<FlatDTO> flatDTOs = flats.stream()
                .map(this::convertToDTO)
                .toList();
            
            return Response.ok(flatDTOs).build();
            
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ErrorResponse("Ошибка поиска квартир по названию: " + e.getMessage()))
                .build();
        }
    }
    
    /**
     * Найти квартиры с жилой площадью меньше заданной
     */
    @GET
    @Path("/search/by-living-space-less-than/{maxSpace}")
    public Response findByLivingSpaceLessThan(@PathParam("maxSpace") Long maxSpace) {
        try {
            if (maxSpace == null || maxSpace <= 0) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("Максимальная жилая площадь должна быть больше 0"))
                    .build();
            }
            
            List<com.arekalov.islab1.pojo.Flat> flats = flatService.findByLivingSpaceLessThan(maxSpace);
            List<FlatDTO> flatDTOs = flats.stream()
                .map(this::convertToDTO)
                .toList();
            
            return Response.ok(flatDTOs).build();
            
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ErrorResponse("Ошибка поиска квартир по жилой площади: " + e.getMessage()))
                .build();
        }
    }
    
    /**
     * Найти самую дешевую квартиру с балконом
     */
    @GET
    @Path("/search/cheapest-with-balcony")
    public Response findCheapestWithBalcony() {
        try {
            com.arekalov.islab1.pojo.Flat flat = flatService.findCheapestWithBalcony();
            
            if (flat != null) {
                FlatDTO flatDTO = convertToDTO(flat);
                return Response.ok(flatDTO).build();
            } else {
                return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorResponse("Квартиры с балконом не найдены"))
                    .build();
            }
            
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ErrorResponse("Ошибка поиска самой дешевой квартиры с балконом: " + e.getMessage()))
                .build();
        }
    }
    
    /**
     * Получить все квартиры, отсортированные по времени до метро пешком
     */
    @GET
    @Path("/sorted-by-metro-time")
    public Response findAllSortedByMetroTime() {
        try {
            List<com.arekalov.islab1.pojo.Flat> flats = flatService.findAllSortedByMetroTime();
            List<FlatDTO> flatDTOs = flats.stream()
                .map(this::convertToDTO)
                .toList();
            
            return Response.ok(flatDTOs).build();
            
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ErrorResponse("Ошибка получения квартир, отсортированных по времени до метро: " + e.getMessage()))
                .build();
        }
    }
    
    /**
     * Конвертировать Entity в DTO
     */
    private FlatDTO convertToDTO(com.arekalov.islab1.pojo.Flat flat) {
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
    private com.arekalov.islab1.pojo.Flat convertFromRequest(CreateFlatRequest request) {
        com.arekalov.islab1.pojo.Flat flat = new com.arekalov.islab1.pojo.Flat();
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
            com.arekalov.islab1.pojo.Coordinates coords = new com.arekalov.islab1.pojo.Coordinates();
            coords.setX(request.getCoordinates().getX());
            coords.setY(request.getCoordinates().getY());
            flat.setCoordinates(coords);
        }
        
        // Конвертируем дом (если указан ID)
        if (request.getHouseId() != null) {
            com.arekalov.islab1.pojo.House house = new com.arekalov.islab1.pojo.House();
            house.setId(request.getHouseId());
            flat.setHouse(house);
        }
        
        return flat;
    }
}
