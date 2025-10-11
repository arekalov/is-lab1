package com.arekalov.islab1.config;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import javax.naming.InitialContext;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Logger;

/**
 * Конфигурация для прямого доступа к DataSource (чистый JDBC без ORM)
 */
@ApplicationScoped
public class DataSourceConfig {
    
    private static final Logger logger = Logger.getLogger(DataSourceConfig.class.getName());
    
    private DataSource dataSource;
    
    @PostConstruct
    public void init() {
        try {
            logger.info("Инициализация DataSource...");
            
            // Получаем DataSource из JNDI
            InitialContext ctx = new InitialContext();
            dataSource = (DataSource) ctx.lookup("java:jboss/datasources/flatsPu");
            
            // Проверяем подключение
            try (Connection conn = dataSource.getConnection()) {
                logger.info("Подключение к БД успешно установлено");
            }
            
        } catch (Exception e) {
            logger.severe("Ошибка инициализации DataSource: " + e.getMessage());
            throw new RuntimeException("Не удалось инициализировать DataSource", e);
        }
    }
    
    @PreDestroy
    public void destroy() {
        logger.info("Завершение работы с DataSource...");
    }
    
    public DataSource getDataSource() {
        return dataSource;
    }
    
    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }
}
