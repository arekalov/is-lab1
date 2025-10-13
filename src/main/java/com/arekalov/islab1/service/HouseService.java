package com.arekalov.islab1.service;

import com.arekalov.islab1.dto.request.CreateHouseRequest;
import com.arekalov.islab1.dto.HouseDTO;
import com.arekalov.islab1.pojo.House;
import com.arekalov.islab1.repository.HouseRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;
import java.util.stream.Collectors;
import java.util.logging.Logger;

/**
 * Сервис для работы с домами через EclipseLink (БЕЗ ТРАНЗАКЦИЙ, БЕЗ JPA!)
 */
@ApplicationScoped
public class HouseService {
    
    private static final Logger logger = Logger.getLogger(HouseService.class.getName());
    
    @Inject
    private HouseRepository houseRepository;
    
    @Inject
    private WebSocketService webSocketService;
    
    /**
     * Получить все дома с пагинацией
     */
    public List<HouseDTO> getAllHouses(int page, int size) {
        return houseRepository.findAll(page, size).stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
    }
    
    /**
     * Получить общее количество домов
     */
    public long countHouses() {
        return houseRepository.count();
    }
    
    /**
     * Получить все дома (без пагинации) - для обратной совместимости
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
        logger.info("HouseService.createHouse() - начало, name=" + request.getName());
        
        try {
            // Создаем объект House с данными из запроса
            House house = new House(request.getName(), request.getYear(), request.getNumberOfFlatsOnFloor());
            logger.info("HouseService.createHouse() - создан объект House, name=" + house.getName());
            
            // Сохраняем через EclipseLink (БЕЗ ТРАНЗАКЦИЙ!)
            logger.info("HouseService.createHouse() - сохраняем через EclipseLink");
            House savedHouse = houseRepository.save(house);
            
            logger.info("HouseService.createHouse() - после сохранения, id=" + savedHouse.getId());
            
            // Возвращаем DTO с реальными данными
            HouseDTO responseDTO = toDTO(savedHouse);
            logger.info("HouseService.createHouse() - возвращаем DTO, id=" + responseDTO.getId());
            
            // Отправляем уведомление через WebSocket
            webSocketService.notifyHouseUpdate("CREATE", responseDTO);
            
            return responseDTO;
            
        } catch (Exception e) {
            logger.severe("HouseService.createHouse() - ошибка: " + e.getMessage());
            throw new RuntimeException("Ошибка создания дома: " + e.getMessage(), e);
        }
    }
    
    /**
     * Обновить дом (БЕЗ ТРАНЗАКЦИЙ)
     */
    public HouseDTO updateHouse(Long id, CreateHouseRequest request) {
        logger.info("HouseService.updateHouse() - начало, id=" + id + ", name=" + request.getName());
        
        try {
            // Находим существующий дом
            House existingHouse = houseRepository.findById(id);
            if (existingHouse == null) {
                logger.info("HouseService.updateHouse() - дом с id=" + id + " не найден");
                return null;
            }
            
            // Обновляем поля
            existingHouse.setName(request.getName());
            existingHouse.setYear(request.getYear());
            existingHouse.setNumberOfFlatsOnFloor(request.getNumberOfFlatsOnFloor());
            
            // Сохраняем изменения через EclipseLink
            logger.info("HouseService.updateHouse() - сохраняем изменения через EclipseLink");
            House updatedHouse = houseRepository.save(existingHouse);
            
            // Возвращаем DTO
            HouseDTO responseDTO = toDTO(updatedHouse);
            logger.info("HouseService.updateHouse() - возвращаем DTO, id=" + responseDTO.getId());
            
            // Отправляем уведомление через WebSocket
            webSocketService.notifyHouseUpdate("UPDATE", responseDTO);
            
            return responseDTO;
            
        } catch (Exception e) {
            logger.severe("HouseService.updateHouse() - ошибка: " + e.getMessage());
            throw new RuntimeException("Ошибка обновления дома: " + e.getMessage(), e);
        }
    }
    
    /**
     * Удалить дом (с каскадным удалением квартир)
     */
    public boolean deleteHouse(Long id) {
        logger.info("HouseService.deleteHouse() - начало каскадного удаления дома с id=" + id);
        
        try {
            // Проверяем количество квартир для информирования пользователя
            long flatsCount = houseRepository.getFlatsCount(id);
            if (flatsCount > 0) {
                logger.info("HouseService.deleteHouse() - будет выполнено каскадное удаление " + flatsCount + " квартир(ы)");
            }
            
            // Удаляем дом с каскадным удалением квартир через EclipseLink
            logger.info("HouseService.deleteHouse() - выполняем каскадное удаление через EclipseLink");
            boolean deleted = houseRepository.deleteById(id);
            
            if (deleted) {
                if (flatsCount > 0) {
                    logger.info("HouseService.deleteHouse() - дом и " + flatsCount + " связанных квартир(ы) успешно удалены");
                } else {
                    logger.info("HouseService.deleteHouse() - дом успешно удален (квартир не было)");
                }
                // Отправляем уведомление через WebSocket
                webSocketService.notifyHouseUpdate("DELETE", id);
                return true;
            } else {
                logger.info("HouseService.deleteHouse() - дом не найден для удаления");
                return false;
            }
            
        } catch (Exception e) {
            logger.severe("HouseService.deleteHouse() - ошибка каскадного удаления: " + e.getMessage());
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
     * Получить количество квартир в доме
     */
    public long getFlatsCount(Long houseId) {
        return houseRepository.getFlatsCount(houseId);
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
