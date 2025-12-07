package com.arekalov.islab1.repository;

import com.arekalov.islab1.entity.Flat;
import com.arekalov.islab1.entity.Coordinates;
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
 * Репозиторий для работы с квартирами через JPA API
 */
@ApplicationScoped
public class FlatRepository {
    
    private static final Logger logger = Logger.getLogger(FlatRepository.class.getName());
    
    @Inject
    private EntityManagerService entityManagerService;
    
    /**
     * Получить EntityManager
     */
    private EntityManager getEntityManager() {
        return entityManagerService.getEntityManager();
    }
    
    /**
     * Найти все квартиры с пагинацией
     */
    public List<Flat> findAll(int page, int size, String sortBy) {
        logger.info("FlatRepository.findAll() - поиск квартир: page=" + page + ", size=" + size + ", sortBy=" + sortBy);
        
        // Валидация параметров
        if (page < 0) {
            logger.warning("FlatRepository.findAll() - page не может быть отрицательным: " + page);
            page = 0;
        }
        if (size <= 0) {
            logger.warning("FlatRepository.findAll() - size должен быть положительным: " + size);
            size = 10;
        }
        if (size > 100) {
            logger.warning("FlatRepository.findAll() - size слишком большой: " + size);
            size = 100;
        }
        
        try {
            EntityManager em = getEntityManager();
            
            // Определяем поле сортировки
            String orderByField = "id";
            if ("name".equals(sortBy)) {
                orderByField = "name";
            } else if ("price".equals(sortBy)) {
                orderByField = "price";
            } else if ("area".equals(sortBy)) {
                orderByField = "area";
            }
            
            String jpql = "SELECT f FROM Flat f ORDER BY f." + orderByField + " ASC";
            TypedQuery<Flat> query = em.createQuery(jpql, Flat.class);
            query.setFirstResult(page * size);
            query.setMaxResults(size);
            
            List<Flat> flats = query.getResultList();
            logger.info("FlatRepository.findAll() - найдено квартир: " + flats.size());
            return flats;
            
        } catch (Exception e) {
            logger.severe("Ошибка поиска квартир: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Error finding flats: " + e.getMessage(), e);
        }
    }
    
    public List<Flat> findAll() {
        return findAll(0, 20, "id");
    }
    
    /**
     * Подсчитать общее количество квартир
     */
    public Long count() {
        logger.info("FlatRepository.count() - подсчет общего количества квартир");
        
        try {
            EntityManager em = getEntityManager();
            TypedQuery<Long> query = em.createQuery("SELECT COUNT(f) FROM Flat f", Long.class);
            Long count = query.getSingleResult();
            logger.info("FlatRepository.count() - общее количество квартир: " + count);
            return count;
            
        } catch (Exception e) {
            logger.severe("Ошибка подсчета квартир: " + e.getMessage());
            throw new RuntimeException("Error counting flats: " + e.getMessage(), e);
        }
    }
    
    /**
     * Найти квартиру по ID
     */
    public Flat findById(Long id) {
        logger.info("FlatRepository.findById() - поиск квартиры с id=" + id);
        
        try {
            EntityManager em = getEntityManager();
            Flat flat = em.find(Flat.class, id);
            
            if (flat != null) {
                logger.info("FlatRepository.findById() - квартира найдена: " + flat.getName());
            } else {
                logger.info("FlatRepository.findById() - квартира не найдена");
            }
            
            return flat;
            
        } catch (Exception e) {
            logger.severe("Ошибка поиска квартиры по ID: " + e.getMessage());
            throw new RuntimeException("Error finding flat by id: " + e.getMessage(), e);
        }
    }
    
    /**
     * Сохранить квартиру с транзакцией
     */
    @Transactional
    public Flat save(Flat flat) {
        logger.info("FlatRepository.save() - сохранение квартиры: " + flat.getName());
        
        try {
            EntityManager em = getEntityManager();
            
            if (flat.getId() == null) {
                // Новая квартира - persist
                em.persist(flat);
                logger.info("FlatRepository.save() - квартира создана с id=" + flat.getId());
            } else {
                // Существующая квартира - merge
                flat = em.merge(flat);
                logger.info("FlatRepository.save() - квартира обновлена с id=" + flat.getId());
            }
            
            return flat;
            
        } catch (Exception e) {
            logger.severe("Ошибка сохранения квартиры: " + e.getMessage());
            throw new RuntimeException("Error saving flat: " + e.getMessage(), e);
        }
    }
    
    /**
     * Сохранить координаты
     */
    public Coordinates saveCoordinates(Coordinates coordinates) {
        logger.info("FlatRepository.saveCoordinates() - сохранение координат");
        
        try {
            EntityManager em = getEntityManager();
            
            if (coordinates.getId() == null) {
                // Новые координаты - persist
                em.persist(coordinates);
                logger.info("FlatRepository.saveCoordinates() - координаты созданы с id=" + coordinates.getId());
            } else {
                // Существующие координаты - merge
                coordinates = em.merge(coordinates);
                logger.info("FlatRepository.saveCoordinates() - координаты обновлены с id=" + coordinates.getId());
            }
            
            return coordinates;
            
        } catch (Exception e) {
            logger.severe("Ошибка сохранения координат: " + e.getMessage());
            throw new RuntimeException("Error saving coordinates: " + e.getMessage(), e);
        }
    }
    
    /**
     * Удалить квартиру по ID с транзакцией
     */
    @Transactional
    public boolean deleteById(Long id) {
        logger.info("FlatRepository.deleteById() - удаление квартиры с id=" + id);
        
        try {
            EntityManager em = getEntityManager();
            
            Flat flat = em.find(Flat.class, id);
            if (flat == null) {
                logger.info("FlatRepository.deleteById() - квартира не найдена для удаления");
                return false;
            }
            
            // Сохраняем координаты для проверки
            Coordinates coordinates = flat.getCoordinates();
            Long coordinatesId = coordinates != null ? coordinates.getId() : null;
            
            // Удаляем квартиру
            em.remove(flat);
            
            // Удаляем координаты, если они не используются другими квартирами
            if (coordinatesId != null && !isCoordinatesUsedByOtherFlats(coordinatesId)) {
                Coordinates coordsToDelete = em.find(Coordinates.class, coordinatesId);
                if (coordsToDelete != null) {
                    em.remove(coordsToDelete);
                    logger.info("FlatRepository.deleteById() - удалены неиспользуемые координаты с id=" + coordinatesId);
                }
            }
            
            logger.info("FlatRepository.deleteById() - квартира успешно удалена");
            return true;
            
        } catch (Exception e) {
            logger.severe("Ошибка удаления квартиры: " + e.getMessage());
            throw new RuntimeException("Error deleting flat: " + e.getMessage(), e);
        }
    }
    
    /**
     * Проверить, используются ли координаты другими квартирами
     */
    private boolean isCoordinatesUsedByOtherFlats(Long coordinatesId) {
        try {
            EntityManager em = getEntityManager();
            TypedQuery<Long> query = em.createQuery(
                "SELECT COUNT(f) FROM Flat f WHERE f.coordinates.id = :coordsId", Long.class);
            query.setParameter("coordsId", coordinatesId);
            Long count = query.getSingleResult();
            return count > 0;
            
        } catch (Exception e) {
            logger.severe("Ошибка проверки использования координат: " + e.getMessage());
            return true; // В случае ошибки лучше не удалять координаты
        }
    }
    
    /**
     * Подсчитать количество квартир с количеством комнат больше заданного
     */
    public Long countByRoomsGreaterThan(Integer minRooms) {
        logger.info("FlatRepository.countByRoomsGreaterThan() - подсчет квартир с комнатами > " + minRooms);
        
        try {
            EntityManager em = getEntityManager();
            TypedQuery<Long> query = em.createQuery(
                "SELECT COUNT(f) FROM Flat f WHERE f.numberOfRooms > :minRooms", Long.class);
            query.setParameter("minRooms", minRooms);
            Long count = query.getSingleResult();
            logger.info("FlatRepository.countByRoomsGreaterThan() - найдено квартир: " + count);
            return count;
            
        } catch (Exception e) {
            logger.severe("Ошибка подсчета квартир по комнатам: " + e.getMessage());
            throw new RuntimeException("Error counting flats by rooms: " + e.getMessage(), e);
        }
    }
    
    /**
     * Найти квартиры, содержащие подстроку в названии
     */
    public List<Flat> findByNameContaining(String nameSubstring) {
        logger.info("FlatRepository.findByNameContaining() - поиск квартир с названием содержащим: " + nameSubstring);
        
        try {
            EntityManager em = getEntityManager();
            String searchString = nameSubstring.trim().toLowerCase();
            
            TypedQuery<Flat> query = em.createQuery(
                "SELECT f FROM Flat f WHERE LOWER(f.name) LIKE :search ORDER BY f.name ASC", Flat.class);
            query.setParameter("search", "%" + searchString + "%");
            
            List<Flat> flats = query.getResultList();
            logger.info("FlatRepository.findByNameContaining() - найдено квартир: " + flats.size());
            return flats;
            
        } catch (Exception e) {
            logger.severe("Ошибка поиска квартир по названию: " + e.getMessage());
            throw new RuntimeException("Error finding flats by name: " + e.getMessage(), e);
        }
    }
    
    /**
     * Найти квартиры с жилой площадью меньше заданной
     */
    public List<Flat> findByLivingSpaceLessThan(Long maxSpace) {
        logger.info("FlatRepository.findByLivingSpaceLessThan() - поиск квартир с площадью < " + maxSpace);
        
        try {
            EntityManager em = getEntityManager();
            TypedQuery<Flat> query = em.createQuery(
                "SELECT f FROM Flat f WHERE f.livingSpace < :maxSpace", Flat.class);
            query.setParameter("maxSpace", maxSpace);
            
            List<Flat> flats = query.getResultList();
            logger.info("FlatRepository.findByLivingSpaceLessThan() - найдено квартир: " + flats.size());
            return flats;
            
        } catch (Exception e) {
            logger.severe("Ошибка поиска квартир по жилой площади: " + e.getMessage());
            throw new RuntimeException("Error finding flats by living space: " + e.getMessage(), e);
        }
    }
    
    /**
     * Найти самую дешевую квартиру с балконом
     */
    public Flat findCheapestWithBalcony() {
        logger.info("FlatRepository.findCheapestWithBalcony() - поиск самой дешевой квартиры с балконом");
        
        try {
            EntityManager em = getEntityManager();
            TypedQuery<Flat> query = em.createQuery(
                "SELECT f FROM Flat f WHERE f.balcony = true ORDER BY f.price ASC", Flat.class);
            query.setMaxResults(1);
            
            List<Flat> flats = query.getResultList();
            if (!flats.isEmpty()) {
                Flat cheapest = flats.get(0);
                logger.info("FlatRepository.findCheapestWithBalcony() - найдена квартира: " + cheapest.getName() + ", цена: " + cheapest.getPrice());
                return cheapest;
            } else {
                logger.info("FlatRepository.findCheapestWithBalcony() - квартиры с балконом не найдены");
                return null;
            }
            
        } catch (Exception e) {
            logger.severe("Ошибка поиска самой дешевой квартиры с балконом: " + e.getMessage());
            throw new RuntimeException("Error finding cheapest flat with balcony: " + e.getMessage(), e);
        }
    }
    
    /**
     * Найти все квартиры, отсортированные по времени до метро
     */
    public List<Flat> findAllSortedByMetroTime() {
        logger.info("FlatRepository.findAllSortedByMetroTime() - поиск всех квартир, отсортированных по времени до метро");
        
        try {
            EntityManager em = getEntityManager();
            TypedQuery<Flat> query = em.createQuery(
                "SELECT f FROM Flat f ORDER BY f.timeToMetroOnFoot ASC", Flat.class);
            
            List<Flat> flats = query.getResultList();
            logger.info("FlatRepository.findAllSortedByMetroTime() - найдено квартир: " + flats.size());
            return flats;
            
        } catch (Exception e) {
            logger.severe("Ошибка поиска квартир, отсортированных по времени до метро: " + e.getMessage());
            throw new RuntimeException("Error finding flats sorted by metro time: " + e.getMessage(), e);
        }
    }
}
