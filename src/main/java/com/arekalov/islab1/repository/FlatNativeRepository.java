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

import javax.naming.InitialContext;
import javax.sql.DataSource;
import java.util.List;
import java.util.logging.Logger;

/**
 * Репозиторий для работы с квартирами на нативном EclipseLink API
 */
@ApplicationScoped
public class FlatNativeRepository {
    
    private static final Logger logger = Logger.getLogger(FlatNativeRepository.class.getName());
    
    @Inject
    private HouseRepository houseRepository; // Используем общую сессию
    
    /**
     * Найти все квартиры с пагинацией через прямой SQL
     */
    public List<Flat> findAll(int page, int size, String sortBy) {
        logger.info("FlatNativeRepository.findAll() - поиск квартир: page=" + page + ", size=" + size + ", sortBy=" + sortBy);
        
        try {
            // Получаем DataSource напрямую из JNDI для выполнения запроса с пагинацией
            InitialContext ctx = new InitialContext();
            DataSource dataSource = (DataSource) ctx.lookup("java:jboss/datasources/flatsPu");
            
            String sql = "SELECT f.id, f.name, f.area, f.price, f.balcony, f.time_to_metro_on_foot, " +
                        "f.number_of_rooms, f.living_space, f.furnish, f.view, f.creation_date, " +
                        "f.coordinates_id, f.house_id " +
                        "FROM flats f ORDER BY " + sortBy + " LIMIT ? OFFSET ?";
            
            List<Flat> flats = new java.util.ArrayList<>();
            
            try (java.sql.Connection connection = dataSource.getConnection();
                 java.sql.PreparedStatement stmt = connection.prepareStatement(sql)) {
                
                stmt.setInt(1, size);
                stmt.setInt(2, page * size);
                
                try (java.sql.ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        Flat flat = new Flat();
                        flat.setId(rs.getLong("id"));
                        flat.setName(rs.getString("name"));
                        flat.setArea((long) rs.getDouble("area"));
                        flat.setPrice(rs.getLong("price"));
                        flat.setBalcony(rs.getBoolean("balcony"));
                        flat.setTimeToMetroOnFoot((long) rs.getInt("time_to_metro_on_foot"));
                        flat.setNumberOfRooms(rs.getInt("number_of_rooms"));
                        flat.setLivingSpace((long) rs.getFloat("living_space"));
                        
                        // Enum fields
                        String furnishStr = rs.getString("furnish");
                        if (furnishStr != null) {
                            flat.setFurnish(com.arekalov.islab1.entity.Furnish.valueOf(furnishStr));
                        }
                        
                        String viewStr = rs.getString("view");
                        if (viewStr != null) {
                            flat.setView(com.arekalov.islab1.entity.View.valueOf(viewStr));
                        }
                        
                        flat.setCreationDate(rs.getTimestamp("creation_date").toLocalDateTime().atZone(java.time.ZoneId.systemDefault()));
                        
                        // Foreign keys - we'll load them separately if needed
                        Long coordinatesId = rs.getLong("coordinates_id");
                        if (!rs.wasNull()) {
                            // Load coordinates separately
                            Coordinates coordinates = findCoordinatesById(coordinatesId);
                            flat.setCoordinates(coordinates);
                        }
                        
                        Long houseId = rs.getLong("house_id");
                        if (!rs.wasNull()) {
                            // We can set just the ID to avoid circular dependency
                            com.arekalov.islab1.entity.House house = new com.arekalov.islab1.entity.House();
                            house.setId(houseId);
                            flat.setHouse(house);
                        }
                        
                        flats.add(flat);
                    }
                }
            }
            
            logger.info("FlatNativeRepository.findAll() - найдено квартир: " + flats.size());
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
        logger.info("FlatNativeRepository.count() - подсчет общего количества квартир");
        
        try {
            InitialContext ctx = new InitialContext();
            DataSource dataSource = (DataSource) ctx.lookup("java:jboss/datasources/flatsPu");
            
            String sql = "SELECT COUNT(*) FROM flats";
            
            try (java.sql.Connection connection = dataSource.getConnection();
                 java.sql.PreparedStatement stmt = connection.prepareStatement(sql)) {
                
                try (java.sql.ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        long count = rs.getLong(1);
                        logger.info("FlatNativeRepository.count() - общее количество квартир: " + count);
                        return count;
                    }
                }
            }
            
            return 0L;
            
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
     * Найти координаты по ID
     */
    private Coordinates findCoordinatesById(Long id) {
        try {
            DatabaseSession session = houseRepository.getDatabaseSession();
            
            ReadObjectQuery query = new ReadObjectQuery(Coordinates.class);
            ExpressionBuilder builder = query.getExpressionBuilder();
            Expression criteria = builder.get("id").equal(id);
            query.setSelectionCriteria(criteria);
            
            return (Coordinates) session.executeQuery(query);
            
        } catch (Exception e) {
            logger.severe("Ошибка поиска координат по ID: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Проверить, используются ли координаты другими квартирами
     */
    private boolean isCoordinatesUsedByOtherFlats(Long coordinatesId) {
        try {
            InitialContext ctx = new InitialContext();
            DataSource dataSource = (DataSource) ctx.lookup("java:jboss/datasources/flatsPu");
            
            String sql = "SELECT COUNT(*) FROM flats WHERE coordinates_id = ?";
            
            try (java.sql.Connection connection = dataSource.getConnection();
                 java.sql.PreparedStatement stmt = connection.prepareStatement(sql)) {
                
                stmt.setLong(1, coordinatesId);
                
                try (java.sql.ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getLong(1) > 1; // Больше 1 означает, что есть другие квартиры
                    }
                }
            }
            
            return false;
            
        } catch (Exception e) {
            logger.severe("Ошибка проверки использования координат: " + e.getMessage());
            return true; // В случае ошибки лучше не удалять координаты
        }
    }
}
