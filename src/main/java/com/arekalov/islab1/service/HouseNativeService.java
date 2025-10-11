package com.arekalov.islab1.service;

import com.arekalov.islab1.dto.CreateHouseRequest;
import com.arekalov.islab1.dto.HouseDTO;
import com.arekalov.islab1.entity.House;
import com.arekalov.islab1.repository.HouseNativeRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;
import java.util.stream.Collectors;
import java.util.logging.Logger;

/**
 * Сервис для работы с домами через нативный EclipseLink (БЕЗ ТРАНЗАКЦИЙ, БЕЗ JPA!)
 */
@ApplicationScoped
public class HouseNativeService {
    
    private static final Logger logger = Logger.getLogger(HouseNativeService.class.getName());
    
    @Inject
    private HouseNativeRepository houseRepository;
    
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
     * Создать новый дом (БЕЗ ТРАНЗАКЦИЙ)
     */
    public HouseDTO createHouse(CreateHouseRequest request) {
        logger.info("HouseNativeService.createHouse() - начало, name=" + request.getName());
        
        try {
            // Создаем объект House с данными из запроса
            House house = new House(request.getName(), request.getYear(), request.getNumberOfFlatsOnFloor());
            logger.info("HouseNativeService.createHouse() - создан объект House, name=" + house.getName());
            
            // Сохраняем через нативный EclipseLink (БЕЗ ТРАНЗАКЦИЙ!)
            logger.info("HouseNativeService.createHouse() - сохраняем через нативный EclipseLink");
            House savedHouse = houseRepository.save(house);
            
            logger.info("HouseNativeService.createHouse() - после сохранения, id=" + savedHouse.getId());
            
            // Возвращаем DTO с реальными данными
            HouseDTO responseDTO = toDTO(savedHouse);
            logger.info("HouseNativeService.createHouse() - возвращаем DTO, id=" + responseDTO.getId());
            
            return responseDTO;
            
        } catch (Exception e) {
            logger.severe("HouseNativeService.createHouse() - ошибка: " + e.getMessage());
            throw new RuntimeException("Ошибка создания дома: " + e.getMessage(), e);
        }
    }
    
    /**
     * Обновить дом (БЕЗ ТРАНЗАКЦИЙ)
     */
    public HouseDTO updateHouse(Long id, CreateHouseRequest request) {
        logger.info("HouseNativeService.updateHouse() - начало, id=" + id + ", name=" + request.getName());
        
        try {
            // Находим существующий дом
            House existingHouse = houseRepository.findById(id);
            if (existingHouse == null) {
                logger.info("HouseNativeService.updateHouse() - дом с id=" + id + " не найден");
                return null;
            }
            
            // Обновляем поля
            existingHouse.setName(request.getName());
            existingHouse.setYear(request.getYear());
            existingHouse.setNumberOfFlatsOnFloor(request.getNumberOfFlatsOnFloor());
            
            // Сохраняем изменения через нативный EclipseLink
            logger.info("HouseNativeService.updateHouse() - сохраняем изменения через нативный EclipseLink");
            House updatedHouse = houseRepository.save(existingHouse);
            
            // Возвращаем DTO
            HouseDTO responseDTO = toDTO(updatedHouse);
            logger.info("HouseNativeService.updateHouse() - возвращаем DTO, id=" + responseDTO.getId());
            
            return responseDTO;
            
        } catch (Exception e) {
            logger.severe("HouseNativeService.updateHouse() - ошибка: " + e.getMessage());
            throw new RuntimeException("Ошибка обновления дома: " + e.getMessage(), e);
        }
    }
    
    /**
     * Удалить дом (БЕЗ ТРАНЗАКЦИЙ)
     */
    public boolean deleteHouse(Long id) {
        logger.info("HouseNativeService.deleteHouse() - начало, id=" + id);
        
        try {
            // Удаляем дом через нативный EclipseLink
            logger.info("HouseNativeService.deleteHouse() - удаляем дом через нативный EclipseLink");
            boolean deleted = houseRepository.deleteById(id);
            
            if (deleted) {
                logger.info("HouseNativeService.deleteHouse() - дом успешно удален");
                return true;
            } else {
                logger.info("HouseNativeService.deleteHouse() - дом не найден для удаления");
                return false;
            }
            
        } catch (Exception e) {
            logger.severe("HouseNativeService.deleteHouse() - ошибка: " + e.getMessage());
            throw new RuntimeException("Ошибка удаления дома: " + e.getMessage(), e);
        }
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
