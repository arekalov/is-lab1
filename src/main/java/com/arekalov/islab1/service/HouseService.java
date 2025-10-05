package com.arekalov.islab1.service;

import com.arekalov.islab1.dto.CreateHouseRequest;
import com.arekalov.islab1.dto.HouseDTO;
import com.arekalov.islab1.entity.House;
import com.arekalov.islab1.repository.HouseRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceUnit;
import java.util.List;
import java.util.stream.Collectors;
import java.util.logging.Logger;

/**
 * Сервис для работы с домами
 */
@ApplicationScoped
public class HouseService {
    
    private static final Logger logger = Logger.getLogger(HouseService.class.getName());
    
    @PersistenceUnit(unitName = "flatsPU")
    private EntityManagerFactory entityManagerFactory;
    
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
        logger.info("HouseService.createHouse() - начало, name=" + request.getName());
        
        EntityManager entityManager = null;
        try {
            // Создаем EntityManager из EntityManagerFactory
            logger.info("HouseService.createHouse() - создаем EntityManager");
            entityManager = entityManagerFactory.createEntityManager();
            
            // Начинаем транзакцию вручную через EntityManager
            logger.info("HouseService.createHouse() - начинаем транзакцию");
            entityManager.getTransaction().begin();
            
            // Создаем объект House с данными из запроса
            House house = new House(request.getName(), request.getYear(), request.getNumberOfFlatsOnFloor());
            logger.info("HouseService.createHouse() - создан объект House, name=" + house.getName());
            
            // Используем Repository, но передаем наш EntityManager
            logger.info("HouseService.createHouse() - сохраняем через Repository");
            House savedHouse = saveHouseWithEntityManager(entityManager, house);
            
            logger.info("HouseService.createHouse() - после сохранения, id=" + savedHouse.getId());
            
            // Коммитим транзакцию
            logger.info("HouseService.createHouse() - коммитим транзакцию");
            entityManager.getTransaction().commit();
            
            // Возвращаем DTO с реальными данными
            HouseDTO responseDTO = toDTO(savedHouse);
            logger.info("HouseService.createHouse() - возвращаем DTO, id=" + responseDTO.getId());
            
            return responseDTO;
            
        } catch (Exception e) {
            logger.severe("HouseService.createHouse() - ошибка: " + e.getMessage());
            try {
                if (entityManager != null && entityManager.getTransaction().isActive()) {
                    logger.info("HouseService.createHouse() - откатываем транзакцию");
                    entityManager.getTransaction().rollback();
                }
            } catch (Exception rollbackEx) {
                logger.severe("HouseService.createHouse() - ошибка отката: " + rollbackEx.getMessage());
            }
            throw new RuntimeException("Ошибка создания дома: " + e.getMessage(), e);
        } finally {
            // Закрываем EntityManager
            if (entityManager != null) {
                logger.info("HouseService.createHouse() - закрываем EntityManager");
                entityManager.close();
            }
        }
    }
    
    /**
     * Вспомогательный метод для сохранения через Repository-подобную логику
     */
    private House saveHouseWithEntityManager(EntityManager entityManager, House house) {
        logger.info("HouseService.saveHouseWithEntityManager() - начало, house.id=" + house.getId() + ", name=" + house.getName());
        if (house.getId() == null) {
            logger.info("HouseService.saveHouseWithEntityManager() - новый объект, вызываем persist()");
            entityManager.persist(house);
            entityManager.flush(); // Принудительно выполняем SQL
            logger.info("HouseService.saveHouseWithEntityManager() - после persist() и flush(), id=" + house.getId());
            return house;
        } else {
            logger.info("HouseService.saveHouseWithEntityManager() - существующий объект, вызываем merge()");
            House merged = entityManager.merge(house);
            entityManager.flush(); // Принудительно выполняем SQL
            logger.info("HouseService.saveHouseWithEntityManager() - после merge() и flush(), id=" + merged.getId());
            return merged;
        }
    }
    
    /**
     * Обновить дом
     */
    public HouseDTO updateHouse(Long id, CreateHouseRequest request) {
        logger.info("HouseService.updateHouse() - начало, id=" + id + ", name=" + request.getName());
        
        EntityManager entityManager = null;
        try {
            // Создаем EntityManager из EntityManagerFactory
            logger.info("HouseService.updateHouse() - создаем EntityManager");
            entityManager = entityManagerFactory.createEntityManager();
            
            // Начинаем транзакцию вручную
            logger.info("HouseService.updateHouse() - начинаем транзакцию");
            entityManager.getTransaction().begin();
            
            // Используем Repository-подобную логику для поиска
            House existingHouse = findHouseByIdWithEntityManager(entityManager, id);
            if (existingHouse == null) {
                logger.info("HouseService.updateHouse() - дом с id=" + id + " не найден");
                return null;
            }
            
            // Обновляем поля
            existingHouse.setName(request.getName());
            existingHouse.setYear(request.getYear());
            existingHouse.setNumberOfFlatsOnFloor(request.getNumberOfFlatsOnFloor());
            
            // Используем Repository-подобную логику для сохранения
            logger.info("HouseService.updateHouse() - сохраняем изменения через Repository");
            House updatedHouse = saveHouseWithEntityManager(entityManager, existingHouse);
            
            // Коммитим транзакцию
            logger.info("HouseService.updateHouse() - коммитим транзакцию");
            entityManager.getTransaction().commit();
            
            // Возвращаем DTO
            HouseDTO responseDTO = toDTO(updatedHouse);
            logger.info("HouseService.updateHouse() - возвращаем DTO, id=" + responseDTO.getId());
            
            return responseDTO;
            
        } catch (Exception e) {
            logger.severe("HouseService.updateHouse() - ошибка: " + e.getMessage());
            try {
                if (entityManager != null && entityManager.getTransaction().isActive()) {
                    logger.info("HouseService.updateHouse() - откатываем транзакцию");
                    entityManager.getTransaction().rollback();
                }
            } catch (Exception rollbackEx) {
                logger.severe("HouseService.updateHouse() - ошибка отката: " + rollbackEx.getMessage());
            }
            throw new RuntimeException("Ошибка обновления дома: " + e.getMessage(), e);
        } finally {
            // Закрываем EntityManager
            if (entityManager != null) {
                logger.info("HouseService.updateHouse() - закрываем EntityManager");
                entityManager.close();
            }
        }
    }
    
    /**
     * Вспомогательный метод для поиска по ID через Repository-подобную логику
     */
    private House findHouseByIdWithEntityManager(EntityManager entityManager, Long id) {
        logger.info("HouseService.findHouseByIdWithEntityManager() - поиск дома с id=" + id);
        House house = entityManager.find(House.class, id);
        logger.info("HouseService.findHouseByIdWithEntityManager() - " + (house != null ? "найден дом: " + house.getName() : "дом не найден"));
        return house;
    }
    
    /**
     * Удалить дом
     */
    public boolean deleteHouse(Long id) {
        logger.info("HouseService.deleteHouse() - начало, id=" + id);
        
        EntityManager entityManager = null;
        try {
            // Создаем EntityManager из EntityManagerFactory
            logger.info("HouseService.deleteHouse() - создаем EntityManager");
            entityManager = entityManagerFactory.createEntityManager();
            
            // Начинаем транзакцию вручную
            logger.info("HouseService.deleteHouse() - начинаем транзакцию");
            entityManager.getTransaction().begin();
            
            // Используем Repository-подобную логику для поиска дома
            House house = findHouseByIdWithEntityManager(entityManager, id);
            if (house == null) {
                logger.info("HouseService.deleteHouse() - дом с id=" + id + " не найден");
                return false;
            }
            
            // Используем Repository-подобную логику для удаления
            logger.info("HouseService.deleteHouse() - удаляем дом через Repository");
            boolean deleted = deleteHouseWithEntityManager(entityManager, house);
            
            if (deleted) {
                // Коммитим транзакцию
                logger.info("HouseService.deleteHouse() - коммитим транзакцию");
                entityManager.getTransaction().commit();
                logger.info("HouseService.deleteHouse() - дом успешно удален");
                return true;
            } else {
                logger.info("HouseService.deleteHouse() - не удалось удалить дом");
                return false;
            }
            
        } catch (Exception e) {
            logger.severe("HouseService.deleteHouse() - ошибка: " + e.getMessage());
            try {
                if (entityManager != null && entityManager.getTransaction().isActive()) {
                    logger.info("HouseService.deleteHouse() - откатываем транзакцию");
                    entityManager.getTransaction().rollback();
                }
            } catch (Exception rollbackEx) {
                logger.severe("HouseService.deleteHouse() - ошибка отката: " + rollbackEx.getMessage());
            }
            throw new RuntimeException("Ошибка удаления дома: " + e.getMessage(), e);
        } finally {
            // Закрываем EntityManager
            if (entityManager != null) {
                logger.info("HouseService.deleteHouse() - закрываем EntityManager");
                entityManager.close();
            }
        }
    }
    
    /**
     * Вспомогательный метод для удаления через Repository-подобную логику
     */
    private boolean deleteHouseWithEntityManager(EntityManager entityManager, House house) {
        try {
            logger.info("HouseService.deleteHouseWithEntityManager() - удаляем дом: " + house.getName());
            // Убедимся, что объект находится в контексте persistence
            if (!entityManager.contains(house)) {
                house = entityManager.merge(house);
            }
            entityManager.remove(house);
            entityManager.flush(); // Принудительно выполняем SQL
            logger.info("HouseService.deleteHouseWithEntityManager() - дом успешно удален");
            return true;
        } catch (Exception e) {
            logger.severe("HouseService.deleteHouseWithEntityManager() - ошибка удаления: " + e.getMessage());
            return false;
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
