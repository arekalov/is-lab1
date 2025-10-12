package com.arekalov.islab1.repository;

import com.arekalov.islab1.entity.Flat;
import com.arekalov.islab1.entity.Coordinates;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.persistence.sessions.DatabaseSession;
import org.eclipse.persistence.queries.ReadAllQuery;
import org.eclipse.persistence.queries.ReadObjectQuery;
import org.eclipse.persistence.expressions.ExpressionBuilder;
import org.eclipse.persistence.expressions.Expression;

import java.util.List;
import java.util.logging.Logger;

/**
 * Репозиторий для работы с квартирами на нативном EclipseLink API
 */
@ApplicationScoped
public class FlatRepository {
    
    private static final Logger logger = Logger.getLogger(FlatRepository.class.getName());
    
    @Inject
    private HouseRepository houseRepository; // Используем общую сессию
    
    /**
     * Найти все квартиры с пагинацией через EclipseLink API
     */
    public List<Flat> findAll(int page, int size, String sortBy) {
        logger.info("FlatRepository.findAll() - поиск квартир: page=" + page + ", size=" + size + ", sortBy=" + sortBy);
        
        try {
            DatabaseSession session = houseRepository.getDatabaseSession();
            
            // Создаем запрос для получения всех квартир
            ReadAllQuery query = new ReadAllQuery(Flat.class);
            
            // Добавляем сортировку
            if ("id".equals(sortBy)) {
                query.addOrdering(query.getExpressionBuilder().get("id").ascending());
            } else if ("name".equals(sortBy)) {
                query.addOrdering(query.getExpressionBuilder().get("name").ascending());
            } else if ("price".equals(sortBy)) {
                query.addOrdering(query.getExpressionBuilder().get("price").ascending());
            } else if ("area".equals(sortBy)) {
                query.addOrdering(query.getExpressionBuilder().get("area").ascending());
            }
            
            // Устанавливаем лимит и смещение для пагинации
            query.setMaxRows(size);
            query.setFirstResult(page * size);
            
            @SuppressWarnings("unchecked")
            List<Flat> flats = (List<Flat>) session.executeQuery(query);
            
            logger.info("FlatRepository.findAll() - найдено квартир: " + flats.size());
            return flats;
            
        } catch (Exception e) {
            logger.severe("Ошибка поиска квартир: " + e.getMessage());
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
            DatabaseSession session = houseRepository.getDatabaseSession();
            
            ReadAllQuery query = new ReadAllQuery(Flat.class);
            
            @SuppressWarnings("unchecked")
            List<Flat> flats = (List<Flat>) session.executeQuery(query);
            
            long count = flats.size();
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
        logger.info("FlatNativeRepository.findById() - поиск квартиры с id=" + id);
        
        try {
            DatabaseSession session = houseRepository.getDatabaseSession();
            
            ReadObjectQuery query = new ReadObjectQuery(Flat.class);
            ExpressionBuilder builder = query.getExpressionBuilder();
            Expression criteria = builder.get("id").equal(id);
            query.setSelectionCriteria(criteria);
            
            Flat flat = (Flat) session.executeQuery(query);
            
            if (flat != null) {
                logger.info("FlatNativeRepository.findById() - квартира найдена: " + flat.getName());
            } else {
                logger.info("FlatNativeRepository.findById() - квартира не найдена");
            }
            
            return flat;
            
        } catch (Exception e) {
            logger.severe("Ошибка поиска квартиры по ID: " + e.getMessage());
            throw new RuntimeException("Error finding flat by id: " + e.getMessage(), e);
        }
    }
    
    /**
     * Сохранить квартиру
     */
    public Flat save(Flat flat) {
        logger.info("FlatNativeRepository.save() - сохранение квартиры: " + flat.getName());
        
        try {
            DatabaseSession session = houseRepository.getDatabaseSession();
            
            // Сначала сохраняем координаты, если они новые
            if (flat.getCoordinates() != null && flat.getCoordinates().getId() == null) {
                session.insertObject(flat.getCoordinates());
            }
            
            // Сохраняем квартиру
            if (flat.getId() == null) {
                session.insertObject(flat);
                logger.info("FlatNativeRepository.save() - квартира создана с id=" + flat.getId());
            } else {
                session.updateObject(flat);
                logger.info("FlatNativeRepository.save() - квартира обновлена с id=" + flat.getId());
            }
            
            return flat;
            
        } catch (Exception e) {
            logger.severe("Ошибка сохранения квартиры: " + e.getMessage());
            throw new RuntimeException("Error saving flat: " + e.getMessage(), e);
        }
    }
    
    /**
     * Удалить квартиру по ID
     */
    public boolean deleteById(Long id) {
        logger.info("FlatNativeRepository.deleteById() - удаление квартиры с id=" + id);
        
        try {
            DatabaseSession session = houseRepository.getDatabaseSession();
            
            // Сначала найдем квартиру
            Flat flat = findById(id);
            if (flat == null) {
                logger.info("FlatNativeRepository.deleteById() - квартира не найдена для удаления");
                return false;
            }
            
            // Удаляем квартиру
            session.deleteObject(flat);
            
            // Удаляем связанные координаты, если они есть и не используются другими квартирами
            if (flat.getCoordinates() != null) {
                Long coordinatesId = flat.getCoordinates().getId();
                if (coordinatesId != null && !isCoordinatesUsedByOtherFlats(coordinatesId)) {
                    session.deleteObject(flat.getCoordinates());
                    logger.info("FlatNativeRepository.deleteById() - удалены неиспользуемые координаты с id=" + coordinatesId);
                }
            }
            
            logger.info("FlatNativeRepository.deleteById() - квартира успешно удалена");
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
            DatabaseSession session = houseRepository.getDatabaseSession();
            
            ReadAllQuery query = new ReadAllQuery(Flat.class);
            ExpressionBuilder builder = query.getExpressionBuilder();
            Expression criteria = builder.get("coordinates").get("id").equal(coordinatesId);
            query.setSelectionCriteria(criteria);
            
            @SuppressWarnings("unchecked")
            List<Flat> flats = (List<Flat>) session.executeQuery(query);
            
            return flats.size() > 1; // Больше 1 означает, что есть другие квартиры
            
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
            DatabaseSession session = houseRepository.getDatabaseSession();
            
            ReadAllQuery query = new ReadAllQuery(Flat.class);
            ExpressionBuilder builder = query.getExpressionBuilder();
            Expression criteria = builder.get("numberOfRooms").greaterThan(minRooms);
            query.setSelectionCriteria(criteria);
            
            @SuppressWarnings("unchecked")
            List<Flat> flats = (List<Flat>) session.executeQuery(query);
            
            long count = flats.size();
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
            DatabaseSession session = houseRepository.getDatabaseSession();
            
            ReadAllQuery query = new ReadAllQuery(Flat.class);
            ExpressionBuilder builder = query.getExpressionBuilder();
            Expression criteria = builder.get("name").like("%" + nameSubstring + "%");
            query.setSelectionCriteria(criteria);
            
            @SuppressWarnings("unchecked")
            List<Flat> flats = (List<Flat>) session.executeQuery(query);
            
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
            DatabaseSession session = houseRepository.getDatabaseSession();
            
            ReadAllQuery query = new ReadAllQuery(Flat.class);
            ExpressionBuilder builder = query.getExpressionBuilder();
            Expression criteria = builder.get("livingSpace").lessThan(maxSpace);
            query.setSelectionCriteria(criteria);
            
            @SuppressWarnings("unchecked")
            List<Flat> flats = (List<Flat>) session.executeQuery(query);
            
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
            DatabaseSession session = houseRepository.getDatabaseSession();
            
            ReadAllQuery query = new ReadAllQuery(Flat.class);
            ExpressionBuilder builder = query.getExpressionBuilder();
            Expression criteria = builder.get("balcony").equal(true);
            query.setSelectionCriteria(criteria);
            query.addOrdering(builder.get("price").ascending());
            query.setMaxRows(1);
            
            @SuppressWarnings("unchecked")
            List<Flat> flats = (List<Flat>) session.executeQuery(query);
            
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
            DatabaseSession session = houseRepository.getDatabaseSession();
            
            ReadAllQuery query = new ReadAllQuery(Flat.class);
            ExpressionBuilder builder = query.getExpressionBuilder();
            query.addOrdering(builder.get("timeToMetroOnFoot").ascending());
            
            @SuppressWarnings("unchecked")
            List<Flat> flats = (List<Flat>) session.executeQuery(query);
            
            logger.info("FlatRepository.findAllSortedByMetroTime() - найдено квартир: " + flats.size());
            return flats;
            
        } catch (Exception e) {
            logger.severe("Ошибка поиска квартир, отсортированных по времени до метро: " + e.getMessage());
            throw new RuntimeException("Error finding flats sorted by metro time: " + e.getMessage(), e);
        }
    }
}
