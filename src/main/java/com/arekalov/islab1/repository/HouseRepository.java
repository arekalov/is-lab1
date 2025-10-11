package com.arekalov.islab1.repository;

import org.eclipse.persistence.sessions.DatabaseSession;
import org.eclipse.persistence.sessions.DatabaseLogin;
import org.eclipse.persistence.sessions.Project;
import org.eclipse.persistence.queries.ReadAllQuery;
import org.eclipse.persistence.queries.ReadObjectQuery;
import org.eclipse.persistence.expressions.ExpressionBuilder;
import org.eclipse.persistence.platform.database.PostgreSQLPlatform;
import org.eclipse.persistence.descriptors.RelationalDescriptor;
import org.eclipse.persistence.mappings.DirectToFieldMapping;
import org.eclipse.persistence.sequencing.NativeSequence;
import org.eclipse.persistence.logging.SessionLog;

import com.arekalov.islab1.entity.House;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import javax.naming.InitialContext;
import javax.sql.DataSource;
import java.util.List;
import java.util.logging.Logger;

/**
 * Repository для работы с домами через EclipseLink API (БЕЗ JPA, БЕЗ ТРАНЗАКЦИЙ!)
 */
@ApplicationScoped
public class HouseRepository {
    
    private static final Logger logger = Logger.getLogger(HouseRepository.class.getName());
    
    private DatabaseSession databaseSession;
    
    @PostConstruct
    public void init() {
        try {
            logger.info("Инициализация EclipseLink...");
            
            // Получаем DataSource из JNDI
            InitialContext ctx = new InitialContext();
            DataSource dataSource = (DataSource) ctx.lookup("java:jboss/datasources/flatsPu");
            
            // Создаем DatabaseLogin
            DatabaseLogin login = new DatabaseLogin();
            login.useExternalConnectionPooling();
            login.setConnector(new org.eclipse.persistence.sessions.JNDIConnector(dataSource));
            login.usePlatform(new PostgreSQLPlatform());
            
            // Настройки для отключения транзакций
            login.setUsesExternalTransactionController(false);
            login.setShouldBindAllParameters(false);
            login.setUsesJDBCBatchWriting(false);
            
            // Создаем проект
            Project project = new Project();
            project.setLogin(login);
            
            // Добавляем дескриптор для House
            project.addDescriptor(buildHouseDescriptor());
            
            // Создаем и логинимся в сессию
            databaseSession = project.createDatabaseSession();
            databaseSession.setLogLevel(SessionLog.INFO);
            
            // Настраиваем последовательность для автогенерации ID (PostgreSQL SERIAL)
            NativeSequence sequence = new NativeSequence("houses_id_seq", 1);
            databaseSession.getLogin().addSequence(sequence);
            
            databaseSession.login();
            
            logger.info("EclipseLink Session успешно инициализирован");
            
        } catch (Exception e) {
            logger.severe("Ошибка инициализации EclipseLink Session: " + e.getMessage());
            throw new RuntimeException("Failed to initialize EclipseLink Session", e);
        }
    }
    
    /**
     * Создаем дескриптор для маппинга House
     */
    private RelationalDescriptor buildHouseDescriptor() {
        RelationalDescriptor descriptor = new RelationalDescriptor();
        descriptor.setJavaClass(House.class);
        descriptor.setTableName("houses");
        
        // ID mapping с автогенерацией
        DirectToFieldMapping idMapping = new DirectToFieldMapping();
        idMapping.setAttributeName("id");
        idMapping.setFieldName("houses.id");
        idMapping.setIsReadOnly(false);
        descriptor.addMapping(idMapping);
        
        // Настраиваем автогенерацию ID для PostgreSQL
        descriptor.setSequenceNumberFieldName("houses.id");
        descriptor.setSequenceNumberName("houses_id_seq"); // Имя последовательности PostgreSQL
        
        // Name mapping
        DirectToFieldMapping nameMapping = new DirectToFieldMapping();
        nameMapping.setAttributeName("name");
        nameMapping.setFieldName("houses.name");
        descriptor.addMapping(nameMapping);
        
        // Year mapping
        DirectToFieldMapping yearMapping = new DirectToFieldMapping();
        yearMapping.setAttributeName("year");
        yearMapping.setFieldName("houses.year");
        descriptor.addMapping(yearMapping);
        
        // NumberOfFlatsOnFloor mapping
        DirectToFieldMapping flatsMapping = new DirectToFieldMapping();
        flatsMapping.setAttributeName("numberOfFlatsOnFloor");
        flatsMapping.setFieldName("houses.number_of_flats_on_floor");
        descriptor.addMapping(flatsMapping);
        
        // Настраиваем primary key
        descriptor.addPrimaryKeyFieldName("houses.id");
        
        return descriptor;
    }
    
    /**
     * Сохранить дом без JPA и без транзакций
     */
    public House save(House house) {
        logger.info("HouseRepository.save() - сохранение дома: " + house.getName());
        
        try {
            if (house.getId() == null) {
                // INSERT - EclipseLink автоматически сгенерирует ID
                logger.info("HouseRepository.save() - выполняем INSERT");
                databaseSession.insertObject(house);
            } else {
                // UPDATE
                logger.info("HouseRepository.save() - выполняем UPDATE для id=" + house.getId());
                databaseSession.updateObject(house);
            }
            
            logger.info("HouseRepository.save() - дом сохранен, id=" + house.getId());
            return house;
            
        } catch (Exception e) {
            logger.severe("Ошибка сохранения дома: " + e.getMessage());
            throw new RuntimeException("Error saving house: " + e.getMessage(), e);
        }
    }
    
    /**
     * Найти все дома
     */
    @SuppressWarnings("unchecked")
    public List<House> findAll() {
        logger.info("HouseRepository.findAll() - поиск всех домов");
        
        try {
            ReadAllQuery query = new ReadAllQuery(House.class);
            query.addOrdering(query.getExpressionBuilder().get("id"));
            
            List<House> houses = (List<House>) databaseSession.executeQuery(query);
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
            ReadObjectQuery query = new ReadObjectQuery(House.class);
            query.setSelectionCriteria(new ExpressionBuilder().get("id").equal(id));
            
            House house = (House) databaseSession.executeQuery(query);
            
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
            databaseSession.deleteObject(house);
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
            ReadAllQuery query = new ReadAllQuery(House.class);
            query.setSelectionCriteria(
                new ExpressionBuilder().get("name").toLowerCase()
                    .like("%" + substring.toLowerCase() + "%")
            );
            
            List<House> houses = (List<House>) databaseSession.executeQuery(query);
            logger.info("HouseRepository.findByNameContaining() - найдено домов: " + houses.size());
            
            return houses;
            
        } catch (Exception e) {
            logger.severe("Ошибка поиска домов по названию: " + e.getMessage());
            throw new RuntimeException("Error finding houses by name: " + e.getMessage(), e);
        }
    }
    
    @PreDestroy
    public void cleanup() {
        if (databaseSession != null && databaseSession.isConnected()) {
            logger.info("Закрытие EclipseLink Session...");
            databaseSession.logout();
        }
    }
}
