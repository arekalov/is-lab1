package com.arekalov.islab1.service;

import com.arekalov.islab1.dto.CreateHouseRequest;
import com.arekalov.islab1.dto.HouseDTO;
import com.arekalov.islab1.entity.House;
import com.arekalov.islab1.repository.HouseRepository;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Сервис для работы с домами
 */
@Stateless
@Transactional
public class HouseService {
    
    @Inject
    private HouseRepository houseRepository;
    
    /**
     * Получить все дома
     */
    public List<HouseDTO> getAllHouses() {
        return houseRepository.findAll().stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
    }
    
    /**
     * Получить дом по ID
     */
    public HouseDTO getHouseById(Long id) {
        House house = houseRepository.findById(id);
        return house != null ? toDTO(house) : null;
    }
    
    /**
     * Создать новый дом
     */
    public HouseDTO createHouse(CreateHouseRequest request) {
        House house = new House(request.getName(), request.getYear(), request.getNumberOfFlatsOnFloor());
        House savedHouse = houseRepository.save(house);
        return toDTO(savedHouse);
    }
    
    /**
     * Обновить дом
     */
    public HouseDTO updateHouse(Long id, CreateHouseRequest request) {
        House existingHouse = houseRepository.findById(id);
        if (existingHouse == null) {
            return null;
        }
        
        existingHouse.setName(request.getName());
        existingHouse.setYear(request.getYear());
        existingHouse.setNumberOfFlatsOnFloor(request.getNumberOfFlatsOnFloor());
        
        House savedHouse = houseRepository.save(existingHouse);
        return toDTO(savedHouse);
    }
    
    /**
     * Удалить дом
     */
    public boolean deleteHouse(Long id) {
        return houseRepository.deleteById(id);
    }
    
    /**
     * Поиск домов по названию
     */
    public List<HouseDTO> findByNameContaining(String substring) {
        return houseRepository.findByNameContaining(substring).stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
    }
    
    /**
     * Конвертация в DTO
     */
    private HouseDTO toDTO(House house) {
        return new HouseDTO(
            house.getId(),
            house.getName(),
            house.getYear(),
            house.getNumberOfFlatsOnFloor()
        );
    }
}
