package com.arekalov.islab1.repository;

import org.eclipse.persistence.sessions.DatabaseSession;
import org.eclipse.persistence.queries.ReadAllQuery;
import org.eclipse.persistence.queries.ReadObjectQuery;
import org.eclipse.persistence.queries.DataReadQuery;
import org.eclipse.persistence.expressions.ExpressionBuilder;
import org.eclipse.persistence.sessions.DatabaseRecord;

import com.arekalov.islab1.pojo.House;
import com.arekalov.islab1.service.DatabaseSessionService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import javax.naming.InitialContext;
import javax.sql.DataSource;
import java.util.List;
import java.util.ArrayList;
import java.util.logging.Logger;

/**
 * Repository для работы с домами через EclipseLink API (БЕЗ JPA, БЕЗ ТРАНЗАКЦИЙ!)
 * Использует централизованный DatabaseSessionService для управления сессией
 */
@ApplicationScoped
public class HouseRepository {
    
    private static final Logger logger = Logger.getLogger(HouseRepository.class.getName());
    
    @Inject
    private DatabaseSessionService sessionService;
    
    /**
     * Получить активную DatabaseSession
     */
    private DatabaseSession getSession() {
        return sessionService.getDatabaseSession();
    }
    
    /**
     * Сохранить дом без JPA и без транзакций
     */
    public House save(House house) {
        logger.info("HouseRepository.save() - сохранение дома: " + house.getName());
        
        try {
            DatabaseSession session = getSession();
            
            if (house.getId() == null) {
                // INSERT - EclipseLink автоматически сгенерирует ID
                logger.info("HouseRepository.save() - выполняем INSERT");
                session.insertObject(house);
            } else {
                // UPDATE
                logger.info("HouseRepository.save() - выполняем UPDATE для id=" + house.getId());
                session.updateObject(house);
            }
            
            logger.info("HouseRepository.save() - дом сохранен, id=" + house.getId());
            return house;
            
        } catch (Exception e) {
            logger.severe("Ошибка сохранения дома: " + e.getMessage());
            throw new RuntimeException("Error saving house: " + e.getMessage(), e);
        }
    }
    
    /**
     * Найти все дома с пагинацией
     */
    @SuppressWarnings("unchecked")
    public List<House> findAll(int page, int size) {
        logger.info("HouseRepository.findAll() - поиск домов с пагинацией: page=" + page + ", size=" + size);
        
        // Валидация параметров
        if (page < 0) {
            logger.warning("HouseRepository.findAll() - page не может быть отрицательным: " + page);
            page = 0;
        }
        if (size <= 0) {
            logger.warning("HouseRepository.findAll() - size должен быть положительным: " + size);
            size = 10; // default size
        }
        if (size > 100) {
            logger.warning("HouseRepository.findAll() - size слишком большой: " + size);
            size = 100; // max size
        }
        
        try {
            DatabaseSession session = getSession();
            
            // Рассчитываем offset
            int offset = page * size;
            logger.info("HouseRepository.findAll() - рассчитанные параметры: offset=" + offset + ", limit=" + size);
            
            // Создаем прямой SQL запрос с LIMIT и OFFSET
            String sql = "SELECT id, name, year, number_of_flats_on_floor " +
                        "FROM houses ORDER BY id ASC LIMIT " + size + " OFFSET " + offset;
            
            logger.info("HouseRepository.findAll() - выполняем SQL: " + sql);
            
            // Создаем DataReadQuery для выполнения прямого SQL
            DataReadQuery dataQuery = new DataReadQuery();
            dataQuery.setSQLString(sql);
            
            // Выполняем запрос и получаем результат
            @SuppressWarnings("unchecked")
            List<DatabaseRecord> records = (List<DatabaseRecord>) session.executeQuery(dataQuery);
            
            // Конвертируем DatabaseRecord в объекты House
            List<House> houses = new ArrayList<>();
            for (DatabaseRecord record : records) {
                House house = convertRecordToHouse(record);
                if (house != null) {
                    houses.add(house);
                }
            }
            
            logger.info("HouseRepository.findAll() - найдено домов: " + houses.size());
            return houses;
            
        } catch (Exception e) {
            logger.severe("Ошибка поиска домов с пагинацией: " + e.getMessage());
            e.printStackTrace(); // Добавляем stack trace для отладки
            throw new RuntimeException("Error finding houses with pagination: " + e.getMessage(), e);
        }
    }
    
    /**
     * Подсчитать общее количество домов
     */
    public long count() {
        logger.info("HouseRepository.count() - подсчет общего количества домов");
        
        try {
            DatabaseSession session = getSession();
            
            ReadAllQuery query = new ReadAllQuery(House.class);
            @SuppressWarnings("unchecked")
            List<House> houses = (List<House>) session.executeQuery(query);
            
            long count = houses.size();
            logger.info("HouseRepository.count() - общее количество домов: " + count);
            return count;
            
        } catch (Exception e) {
            logger.severe("Ошибка подсчета домов: " + e.getMessage());
            throw new RuntimeException("Error counting houses: " + e.getMessage(), e);
        }
    }
    
    /**
     * Найти все дома (без пагинации) - для обратной совместимости
     */
    @SuppressWarnings("unchecked")
    public List<House> findAll() {
        logger.info("HouseRepository.findAll() - поиск всех домов");
        
        try {
            DatabaseSession session = getSession();
            
            ReadAllQuery query = new ReadAllQuery(House.class);
            query.addOrdering(query.getExpressionBuilder().get("id"));
            
            List<House> houses = (List<House>) session.executeQuery(query);
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
            DatabaseSession session = getSession();
            
            ReadObjectQuery query = new ReadObjectQuery(House.class);
            query.setSelectionCriteria(new ExpressionBuilder().get("id").equal(id));
            
            House house = (House) session.executeQuery(query);
            
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
            // Получаем DataSource напрямую из JNDI для выполнения COUNT запроса
            InitialContext ctx = new InitialContext();
            DataSource dataSource = (DataSource) ctx.lookup("java:jboss/datasources/flatsPu");
            
            String sql = "SELECT COUNT(*) FROM flats WHERE house_id = ?";
            
            try (java.sql.Connection connection = dataSource.getConnection();
                 java.sql.PreparedStatement stmt = connection.prepareStatement(sql)) {
                
                stmt.setLong(1, houseId);
                
                try (java.sql.ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        long flatsCount = rs.getLong(1);
                        logger.info("HouseRepository.getFlatsCount() - найдено квартир: " + flatsCount);
                        return flatsCount;
                    }
                }
            }
            
            return 0;
            
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
    public void deleteFlatsInHouse(Long houseId) {
        logger.info("HouseRepository.deleteFlatsInHouse() - удаление всех квартир в доме с id=" + houseId);
        
        try {
            // Получаем DataSource напрямую из JNDI для выполнения DELETE запроса
            InitialContext ctx = new InitialContext();
            DataSource dataSource = (DataSource) ctx.lookup("java:jboss/datasources/flatsPu");
            
            String sql = "DELETE FROM flats WHERE house_id = ?";
            
            try (java.sql.Connection connection = dataSource.getConnection();
                 java.sql.PreparedStatement stmt = connection.prepareStatement(sql)) {
                
                stmt.setLong(1, houseId);
                
                int deletedCount = stmt.executeUpdate();
                logger.info("HouseRepository.deleteFlatsInHouse() - удалено квартир: " + deletedCount);
            }
            
        } catch (Exception e) {
            logger.severe("Ошибка удаления квартир в доме: " + e.getMessage());
            throw new RuntimeException("Error deleting flats in house: " + e.getMessage(), e);
        }
    }
    
    /**
     * Удалить дом с каскадным удалением связанных квартир
     */
    public boolean deleteById(Long id) {
        logger.info("HouseRepository.deleteById() - каскадное удаление дома с id=" + id);
        
        try {
            DatabaseSession session = getSession();
            
        House house = findById(id);
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
            session.deleteObject(house);
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
    @SuppressWarnings("unchecked")
    public List<House> findByNameContaining(String substring) {
        logger.info("HouseRepository.findByNameContaining() - поиск домов с подстрокой: " + substring);
        
        try {
            DatabaseSession session = getSession();
            
            ReadAllQuery query = new ReadAllQuery(House.class);
            query.setSelectionCriteria(
                new ExpressionBuilder().get("name").toLowerCase()
                    .like("%" + substring.toLowerCase() + "%")
            );
            
            List<House> houses = (List<House>) session.executeQuery(query);
            logger.info("HouseRepository.findByNameContaining() - найдено домов: " + houses.size());
            
            return houses;
            
        } catch (Exception e) {
            logger.severe("Ошибка поиска домов по названию: " + e.getMessage());
            throw new RuntimeException("Error finding houses by name: " + e.getMessage(), e);
        }
    }
    
    /**
     * Конвертирует DatabaseRecord из базы данных в объект House
     */
    private House convertRecordToHouse(DatabaseRecord record) {
        try {
            House house = new House();
            
            // Основные поля
            house.setId(((Number) record.get("id")).longValue());
            house.setName((String) record.get("name"));
            house.setYear(((Number) record.get("year")).intValue());
            house.setNumberOfFlatsOnFloor(((Number) record.get("number_of_flats_on_floor")).intValue());
            
            return house;
            
        } catch (Exception e) {
            logger.severe("Ошибка конвертации Record в House: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Получить DatabaseSession для использования в других репозиториях
     * @deprecated Используйте DatabaseSessionService напрямую
     */
    @Deprecated
    public DatabaseSession getDatabaseSession() {
        return getSession();
    }
}