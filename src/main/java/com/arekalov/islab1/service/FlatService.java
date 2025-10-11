package com.arekalov.islab1.service;

import com.arekalov.islab1.dto.*;
import com.arekalov.islab1.entity.*;
import com.arekalov.islab1.repository.FlatRepository;
import com.arekalov.islab1.repository.HouseRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
// import jakarta.transaction.Transactional; // Временно отключено для нативного EclipseLink
import java.util.List;
import java.util.stream.Collectors;

/**
 * Сервис для работы с квартирами
 */
@ApplicationScoped
// @Transactional // Временно отключено для нативного EclipseLink
public class FlatService {
    
    @Inject
    private FlatRepository flatRepository;
    
    @Inject
    private HouseRepository houseRepository;
    
    /**
     * Получить все квартиры с пагинацией
     */
    public PagedResponse<FlatDTO> getAllFlats(int page, int size, String sortBy,
                                             String nameFilter, Long minPrice, Long maxPrice,
                                             Boolean hasBalcony, Integer minRooms, Integer maxRooms) {
        
        List<Flat> flats;
        if (nameFilter != null || minPrice != null || maxPrice != null || 
            hasBalcony != null || minRooms != null || maxRooms != null) {
            flats = flatRepository.findWithFilters(nameFilter, minPrice, maxPrice,
                                                  hasBalcony, minRooms, maxRooms,
                                                  page, size, sortBy, "ASC");
        } else {
            flats = flatRepository.findAll(page, size, sortBy);
        }
        
        List<FlatDTO> flatDTOs = flats.stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
        
        Long totalElements = flatRepository.count();
        
        return PagedResponse.of(flatDTOs, page, size, totalElements);
    }
    
    // Перегруженный метод с дефолтными значениями
    public PagedResponse<FlatDTO> getAllFlats() {
        return getAllFlats(0, 20, "id", null, null, null, null, null, null);
    }
    
    /**
     * Получить квартиру по ID
     */
    public FlatDTO getFlatById(Long id) {
        Flat flat = flatRepository.findById(id);
        return flat != null ? toDTO(flat) : null;
    }
    
    /**
     * Создать новую квартиру
     */
    public FlatDTO createFlat(CreateFlatRequest request) {
        // Создаем координаты
        Coordinates coordinates = new Coordinates(
            request.getCoordinates().getX(),
            request.getCoordinates().getY()
        );
        
        // Создаем квартиру
        Flat flat = new Flat(
            request.getName(),
            coordinates,
            request.getArea(),
            request.getPrice(),
            request.getTimeToMetroOnFoot(),
            request.getNumberOfRooms(),
            request.getLivingSpace(),
            request.getFurnish(),
            request.getView()
        );
        
        flat.setBalcony(request.getBalcony());
        
        // Привязываем дом, если указан
        if (request.getHouseId() != null) {
            House house = houseRepository.findById(request.getHouseId());
            flat.setHouse(house);
        }
        
        Flat savedFlat = flatRepository.save(flat);
        return toDTO(savedFlat);
    }
    
    /**
     * Обновить квартиру
     */
    public FlatDTO updateFlat(Long id, CreateFlatRequest request) {
        Flat existingFlat = flatRepository.findById(id);
        if (existingFlat == null) {
            return null;
        }
        
        // Обновляем координаты
        existingFlat.getCoordinates().setX(request.getCoordinates().getX());
        existingFlat.getCoordinates().setY(request.getCoordinates().getY());
        
        // Обновляем поля квартиры
        existingFlat.setName(request.getName());
        existingFlat.setArea(request.getArea());
        existingFlat.setPrice(request.getPrice());
        existingFlat.setBalcony(request.getBalcony());
        existingFlat.setTimeToMetroOnFoot(request.getTimeToMetroOnFoot());
        existingFlat.setNumberOfRooms(request.getNumberOfRooms());
        existingFlat.setLivingSpace(request.getLivingSpace());
        existingFlat.setFurnish(request.getFurnish());
        existingFlat.setView(request.getView());
        
        // Обновляем дом
        if (request.getHouseId() != null) {
            House house = houseRepository.findById(request.getHouseId());
            existingFlat.setHouse(house);
        } else {
            existingFlat.setHouse(null);
        }
        
        Flat savedFlat = flatRepository.save(existingFlat);
        return toDTO(savedFlat);
    }
    
    /**
     * Удалить квартиру
     */
    public boolean deleteFlat(Long id) {
        return flatRepository.deleteById(id);
    }
    
    /**
     * Подсчитать квартиры с количеством комнат больше заданного
     */
    public Long countByRoomsGreaterThan(Integer minRooms) {
        return flatRepository.countByNumberOfRoomsGreaterThan(minRooms);
    }
    
    /**
     * Найти квартиры по названию
     */
    public List<FlatDTO> findByNameContaining(String substring) {
        return flatRepository.findByNameContaining(substring).stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
    }
    
    /**
     * Найти квартиры с жилой площадью меньше заданной
     */
    public List<FlatDTO> findByLivingSpaceLessThan(Long maxSpace) {
        return flatRepository.findByLivingSpaceLessThan(maxSpace).stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
    }
    
    /**
     * Найти самую дешевую квартиру с балконом
     */
    public FlatDTO findCheapestWithBalcony() {
        Flat flat = flatRepository.findCheapestWithBalcony();
        return flat != null ? toDTO(flat) : null;
    }
    
    /**
     * Найти все квартиры, отсортированные по времени до метро
     */
    public List<FlatDTO> findAllSortedByMetroTime() {
        return flatRepository.findAllSortedByMetroTime().stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
    }
    
    /**
     * Конвертация в DTO
     */
    private FlatDTO toDTO(Flat flat) {
        CoordinatesDTO coordinatesDTO = new CoordinatesDTO(
            flat.getCoordinates().getId(),
            flat.getCoordinates().getX(),
            flat.getCoordinates().getY()
        );
        
        HouseDTO houseDTO = null;
        if (flat.getHouse() != null) {
            houseDTO = new HouseDTO(
                flat.getHouse().getId(),
                flat.getHouse().getName(),
                flat.getHouse().getYear(),
                flat.getHouse().getNumberOfFlatsOnFloor()
            );
        }
        
        return new FlatDTO(
            flat.getId(),
            flat.getName(),
            coordinatesDTO,
            flat.getCreationDate(),
            flat.getArea(),
            flat.getPrice(),
            flat.getBalcony(),
            flat.getTimeToMetroOnFoot(),
            flat.getNumberOfRooms(),
            flat.getLivingSpace(),
            flat.getFurnish(),
            flat.getView(),
            houseDTO
        );
    }
}
