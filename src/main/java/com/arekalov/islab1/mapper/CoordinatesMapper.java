package com.arekalov.islab1.mapper;

import com.arekalov.islab1.dto.request.CreateCoordinatesRequest;
import com.arekalov.islab1.dto.response.CoordinatesResponseDTO;
import com.arekalov.islab1.entity.Coordinates;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Mapper для конвертации между Coordinates entity и DTO
 */
@ApplicationScoped
public class CoordinatesMapper {
    
    /**
     * Конвертировать Entity в Response DTO
     */
    public CoordinatesResponseDTO toResponseDTO(Coordinates coordinates) {
        if (coordinates == null) {
            return null;
        }
        
        CoordinatesResponseDTO dto = new CoordinatesResponseDTO();
        dto.setId(coordinates.getId());
        dto.setX(coordinates.getX());
        dto.setY(coordinates.getY());
        
        return dto;
    }
    
    /**
     * Конвертировать CreateRequest в Entity
     */
    public Coordinates fromRequest(CreateCoordinatesRequest request) {
        if (request == null) {
            return null;
        }
        
        Coordinates coordinates = new Coordinates();
        coordinates.setX(request.getX());
        coordinates.setY(request.getY());
        
        return coordinates;
    }
    
    /**
     * Обновить Entity из Request
     */
    public void updateFromRequest(Coordinates coordinates, CreateCoordinatesRequest request) {
        if (coordinates == null || request == null) {
            return;
        }
        
        coordinates.setX(request.getX());
        coordinates.setY(request.getY());
    }
}
