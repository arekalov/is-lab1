package com.arekalov.islab1.mapper;

import com.arekalov.islab1.dto.request.CreateFlatRequest;
import com.arekalov.islab1.dto.request.UpdateFlatRequest;
import com.arekalov.islab1.dto.response.FlatResponseDTO;
import com.arekalov.islab1.entity.Flat;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Mapper для конвертации между Flat entity и DTO
 */
@ApplicationScoped
public class FlatMapper {
    
    @Inject
    private CoordinatesMapper coordinatesMapper;
    
    @Inject
    private HouseMapper houseMapper;
    
    /**
     * Конвертировать Entity в Response DTO
     */
    public FlatResponseDTO toResponseDTO(Flat flat) {
        if (flat == null) {
            return null;
        }
        
        FlatResponseDTO dto = new FlatResponseDTO();
        dto.setId(flat.getId());
        dto.setName(flat.getName());
        dto.setCreationDate(flat.getCreationDate());
        dto.setArea(flat.getArea());
        dto.setPrice(flat.getPrice());
        dto.setBalcony(flat.getBalcony());
        dto.setTimeToMetroOnFoot(flat.getTimeToMetroOnFoot());
        dto.setNumberOfRooms(flat.getNumberOfRooms());
        dto.setLivingSpace(flat.getLivingSpace());
        dto.setFurnish(flat.getFurnish());
        dto.setView(flat.getView());
        dto.setFloor(flat.getFloor());
        
        // Конвертируем вложенные объекты
        dto.setCoordinates(coordinatesMapper.toResponseDTO(flat.getCoordinates()));
        dto.setHouse(houseMapper.toResponseDTO(flat.getHouse()));
        
        return dto;
    }
    
    /**
     * Конвертировать CreateRequest в Entity
     */
    public Flat fromCreateRequest(CreateFlatRequest request) {
        if (request == null) {
            return null;
        }
        
        Flat flat = new Flat();
        flat.setName(request.getName());
        flat.setArea(request.getArea());
        flat.setPrice(request.getPrice());
        flat.setBalcony(request.getBalcony());
        flat.setTimeToMetroOnFoot(request.getTimeToMetroOnFoot());
        flat.setNumberOfRooms(request.getNumberOfRooms());
        flat.setLivingSpace(request.getLivingSpace());
        flat.setFurnish(request.getFurnish());
        flat.setView(request.getView());
        flat.setFloor(request.getFloor());
        
        // Конвертируем координаты
        flat.setCoordinates(coordinatesMapper.fromRequest(request.getCoordinates()));
        
        // Создаем ссылку на дом, если указан ID
        if (request.getHouseId() != null) {
            flat.setHouse(houseMapper.createReference(request.getHouseId()));
        }
        
        return flat;
    }
    
    /**
     * Конвертировать UpdateRequest в Entity
     */
    public Flat fromUpdateRequest(UpdateFlatRequest request) {
        if (request == null) {
            return null;
        }
        
        Flat flat = new Flat();
        flat.setName(request.getName());
        flat.setArea(request.getArea());
        flat.setPrice(request.getPrice());
        flat.setBalcony(request.getBalcony());
        flat.setTimeToMetroOnFoot(request.getTimeToMetroOnFoot());
        flat.setNumberOfRooms(request.getNumberOfRooms());
        flat.setLivingSpace(request.getLivingSpace());
        flat.setFurnish(request.getFurnish());
        flat.setView(request.getView());
        flat.setFloor(request.getFloor());
        
        // Конвертируем координаты
        flat.setCoordinates(coordinatesMapper.fromRequest(request.getCoordinates()));
        
        // Создаем ссылку на дом, если указан ID
        if (request.getHouseId() != null) {
            flat.setHouse(houseMapper.createReference(request.getHouseId()));
        }
        
        return flat;
    }
    
    /**
     * Обновить Entity из UpdateRequest
     */
    public void updateFromRequest(Flat flat, UpdateFlatRequest request) {
        if (flat == null || request == null) {
            return;
        }
        
        flat.setName(request.getName());
        flat.setArea(request.getArea());
        flat.setPrice(request.getPrice());
        flat.setBalcony(request.getBalcony());
        flat.setTimeToMetroOnFoot(request.getTimeToMetroOnFoot());
        flat.setNumberOfRooms(request.getNumberOfRooms());
        flat.setLivingSpace(request.getLivingSpace());
        flat.setFurnish(request.getFurnish());
        flat.setView(request.getView());
        flat.setFloor(request.getFloor());
        
        // Обновляем координаты
        if (request.getCoordinates() != null) {
            if (flat.getCoordinates() != null) {
                coordinatesMapper.updateFromRequest(flat.getCoordinates(), request.getCoordinates());
            } else {
                flat.setCoordinates(coordinatesMapper.fromRequest(request.getCoordinates()));
            }
        }
        
        // Обновляем дом
        if (request.getHouseId() != null) {
            flat.setHouse(houseMapper.createReference(request.getHouseId()));
        } else {
            flat.setHouse(null);
        }
    }
}
