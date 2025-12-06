package com.arekalov.islab1.mapper;

import com.arekalov.islab1.dto.request.CreateHouseRequest;
import com.arekalov.islab1.dto.request.UpdateHouseRequest;
import com.arekalov.islab1.dto.response.HouseResponseDTO;
import com.arekalov.islab1.entity.House;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Mapper для конвертации между House entity и DTO
 */
@ApplicationScoped
public class HouseMapper {
    
    /**
     * Конвертировать Entity в Response DTO
     */
    public HouseResponseDTO toResponseDTO(House house) {
        if (house == null) {
            return null;
        }
        
        HouseResponseDTO dto = new HouseResponseDTO();
        dto.setId(house.getId());
        dto.setName(house.getName());
        dto.setYear(house.getYear());
        dto.setNumberOfFlatsOnFloor(house.getNumberOfFlatsOnFloor());
        
        return dto;
    }
    
    /**
     * Конвертировать CreateRequest в Entity
     */
    public House fromCreateRequest(CreateHouseRequest request) {
        if (request == null) {
            return null;
        }
        
        House house = new House();
        house.setName(request.getName());
        house.setYear(request.getYear());
        house.setNumberOfFlatsOnFloor(request.getNumberOfFlatsOnFloor());
        
        return house;
    }
    
    /**
     * Конвертировать UpdateRequest в Entity
     */
    public House fromUpdateRequest(UpdateHouseRequest request) {
        if (request == null) {
            return null;
        }
        
        House house = new House();
        house.setName(request.getName());
        house.setYear(request.getYear());
        house.setNumberOfFlatsOnFloor(request.getNumberOfFlatsOnFloor());
        
        return house;
    }
    
    /**
     * Обновить Entity из UpdateRequest
     */
    public void updateFromRequest(House house, UpdateHouseRequest request) {
        if (house == null || request == null) {
            return;
        }
        
        house.setName(request.getName());
        house.setYear(request.getYear());
        house.setNumberOfFlatsOnFloor(request.getNumberOfFlatsOnFloor());
    }
    
    /**
     * Создать ссылку на дом (только с ID, без загрузки из БД)
     */
    public House createReference(Long id) {
        if (id == null) {
            return null;
        }
        
        House house = new House();
        house.setId(id);
        return house;
    }
}
