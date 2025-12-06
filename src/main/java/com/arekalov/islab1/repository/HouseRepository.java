package com.arekalov.islab1.repository;

import com.arekalov.islab1.entity.House;
import com.arekalov.islab1.service.EntityManagerService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.logging.Logger;

/**
 * Репозиторий для работы с домами через JPA API
 */
@ApplicationScoped
public class HouseRepository {
    
    private static final Logger logger = Logger.getLogger(HouseRepository.class.getName());
    
    @Inject
    private EntityManagerService entityManagerService;
    
    /**
     * Получить EntityManager
     */
    private EntityManager getEntityManager() {
        return entityManagerService.getEntityManager();
    }
    
    /**
     * Сохранить дом с транзакцией
     */
    @Transactional
    public House save(House house) {
        logger.info("HouseRepository.save() - сохранение дома: " + house.getName());
        
        try {
            EntityManager em = getEntityManager();
            
            if (house.getId() == null) {
                // Новый дом - persist
                em.persist(house);
                logger.info("HouseRepository.save() - дом создан с id=" + house.getId());
            } else {
                // Существующий дом - merge
                house = em.merge(house);
                logger.info("HouseRepository.save() - дом обновлен с id=" + house.getId());
            }
            
            return house;
            
        } catch (Exception e) {
            logger.severe("Ошибка сохранения дома: " + e.getMessage());
            throw new RuntimeException("Error saving house: " + e.getMessage(), e);
        }
    }
    
    /**
     * Найти все дома с пагинацией
     */
    public List<House> findAll(int page, int size) {
        logger.info("HouseRepository.findAll() - поиск домов с пагинацией: page=" + page + ", size=" + size);
        
        // Валидация параметров
        if (page < 0) {
            logger.warning("HouseRepository.findAll() - page не может быть отрицательным: " + page);
            page = 0;
        }
        if (size <= 0) {
            logger.warning("HouseRepository.findAll() - size должен быть положительным: " + size);
            size = 10;
        }
        if (size > 100) {
            logger.warning("HouseRepository.findAll() - size слишком большой: " + size);
            size = 100;
        }
        
        try {
            EntityManager em = getEntityManager();
            
            TypedQuery<House> query = em.createQuery("SELECT h FROM House h ORDER BY h.id ASC", House.class);
            query.setFirstResult(page * size);
            query.setMaxResults(size);
            
            List<House> houses = query.getResultList();
            logger.info("HouseRepository.findAll() - найдено домов: " + houses.size());
            return houses;
            
        } catch (Exception e) {
            logger.severe("Ошибка поиска домов с пагинацией: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Error finding houses with pagination: " + e.getMessage(), e);
        }
    }
    
    /**
     * Подсчитать общее количество домов
     */
    public long count() {
        logger.info("HouseRepository.count() - подсчет общего количества домов");
        
        try {
            EntityManager em = getEntityManager();
            TypedQuery<Long> query = em.createQuery("SELECT COUNT(h) FROM House h", Long.class);
            Long count = query.getSingleResult();
            logger.info("HouseRepository.count() - общее количество домов: " + count);
            return count;
            
        } catch (Exception e) {
            logger.severe("Ошибка подсчета домов: " + e.getMessage());
            throw new RuntimeException("Error counting houses: " + e.getMessage(), e);
        }
    }
    
    /**
     * Найти все дома (без пагинации)
     */
    public List<House> findAll() {
        logger.info("HouseRepository.findAll() - поиск всех домов");
        
        try {
            EntityManager em = getEntityManager();
            TypedQuery<House> query = em.createQuery("SELECT h FROM House h ORDER BY h.id", House.class);
            List<House> houses = query.getResultList();
            logger.info("HouseRepository.findAll() - найдено домов: " + houses.size());
            return houses;
            
        } catch (Exception e) {
            logger.severe("Ошибка поиска всех домов: " + e.getMessage());
            throw new RuntimeException("Error finding all houses: " + e.getMessage(), e);
        }
    }
    
    /**
     * Найти дом по ID
     */
    public House findById(Long id) {
        logger.info("HouseRepository.findById() - поиск дома с id=" + id);
        
        try {
            EntityManager em = getEntityManager();
            House house = em.find(House.class, id);
            
            if (house != null) {
                logger.info("HouseRepository.findById() - дом найден: " + house.getName());
            } else {
                logger.info("HouseRepository.findById() - дом с id=" + id + " не найден");
            }
            
            return house;
            
        } catch (Exception e) {
            logger.severe("Ошибка поиска дома по ID: " + e.getMessage());
            throw new RuntimeException("Error finding house by id: " + e.getMessage(), e);
        }
    }
    
    /**
     * Получить количество квартир в доме
     */
    public long getFlatsCount(Long houseId) {
        logger.info("HouseRepository.getFlatsCount() - подсчет квартир для дома с id=" + houseId);
        
        try {
            EntityManager em = getEntityManager();
            TypedQuery<Long> query = em.createQuery(
                "SELECT COUNT(f) FROM Flat f WHERE f.house.id = :houseId", Long.class);
            query.setParameter("houseId", houseId);
            Long count = query.getSingleResult();
            logger.info("HouseRepository.getFlatsCount() - найдено квартир: " + count);
            return count;
            
        } catch (Exception e) {
            logger.severe("Ошибка подсчета квартир в доме: " + e.getMessage());
            throw new RuntimeException("Error counting flats in house: " + e.getMessage(), e);
        }
    }
    
    /**
     * Проверить, есть ли квартиры в доме
     */
    public boolean hasFlats(Long houseId) {
        return getFlatsCount(houseId) > 0;
    }
    
    /**
     * Удалить все квартиры в доме
     */
    @Transactional
    public void deleteFlatsInHouse(Long houseId) {
        logger.info("HouseRepository.deleteFlatsInHouse() - удаление всех квартир в доме с id=" + houseId);
        
        try {
            EntityManager em = getEntityManager();
            int deletedCount = em.createQuery("DELETE FROM Flat f WHERE f.house.id = :houseId")
                .setParameter("houseId", houseId)
                .executeUpdate();
            logger.info("HouseRepository.deleteFlatsInHouse() - удалено квартир: " + deletedCount);
            
        } catch (Exception e) {
            logger.severe("Ошибка удаления квартир в доме: " + e.getMessage());
            throw new RuntimeException("Error deleting flats in house: " + e.getMessage(), e);
        }
    }
    
    /**
     * Удалить дом с каскадным удалением связанных квартир
     */
    @Transactional
    public boolean deleteById(Long id) {
        logger.info("HouseRepository.deleteById() - каскадное удаление дома с id=" + id);
        
        try {
            EntityManager em = getEntityManager();
            
            House house = em.find(House.class, id);
            if (house == null) {
                logger.info("HouseRepository.deleteById() - дом не найден для удаления");
                return false;
            }
            
            // Сначала удаляем все квартиры в доме (каскадное удаление)
            long flatsCount = getFlatsCount(id);
            if (flatsCount > 0) {
                logger.info("HouseRepository.deleteById() - найдено " + flatsCount + " квартир(ы) для каскадного удаления");
                deleteFlatsInHouse(id);
                logger.info("HouseRepository.deleteById() - все квартиры в доме удалены");
            }
            
            // Теперь удаляем сам дом
            logger.info("HouseRepository.deleteById() - выполняем DELETE дома");
            em.remove(house);
            logger.info("HouseRepository.deleteById() - дом и все связанные квартиры успешно удалены");
            return true;
            
        } catch (Exception e) {
            logger.severe("Ошибка каскадного удаления дома: " + e.getMessage());
            throw new RuntimeException("Error deleting house with cascade: " + e.getMessage(), e);
        }
    }
    
    /**
     * Поиск по названию
     */
    public List<House> findByNameContaining(String substring) {
        logger.info("HouseRepository.findByNameContaining() - поиск домов с подстрокой: " + substring);
        
        try {
            EntityManager em = getEntityManager();
            TypedQuery<House> query = em.createQuery(
                "SELECT h FROM House h WHERE LOWER(h.name) LIKE :search", House.class);
            query.setParameter("search", "%" + substring.toLowerCase() + "%");
            
            List<House> houses = query.getResultList();
            logger.info("HouseRepository.findByNameContaining() - найдено домов: " + houses.size());
            return houses;
            
        } catch (Exception e) {
            logger.severe("Ошибка поиска домов по названию: " + e.getMessage());
            throw new RuntimeException("Error finding houses by name: " + e.getMessage(), e);
        }
    }
}
