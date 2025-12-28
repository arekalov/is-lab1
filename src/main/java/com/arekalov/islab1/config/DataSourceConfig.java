package com.arekalov.islab1.config;

import org.apache.commons.dbcp2.BasicDataSource;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Named;
import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.logging.Logger;

/**
 * Конфигурация Apache Commons DBCP2 DataSource
 * Создает и настраивает пул соединений с БД
 */
@ApplicationScoped
public class DataSourceConfig {
    
    private static final Logger logger = Logger.getLogger(DataSourceConfig.class.getName());
    private BasicDataSource dataSource;
    
    /**
     * Создает и настраивает DBCP2 DataSource
     */
    @Produces
    @ApplicationScoped
    @Named("dbcp2DataSource")
    public DataSource createDataSource() {
        if (dataSource == null) {
            logger.info("Initializing Apache Commons DBCP2 DataSource");
            
            dataSource = new BasicDataSource();
            
            // Основные параметры подключения
            dataSource.setDriverClassName("org.postgresql.Driver");
            dataSource.setUrl("jdbc:postgresql://pg:5432/studs");
            dataSource.setUsername("s409449");
            dataSource.setPassword("7ChhsEMmi3l6rp3x");
            
            // Параметры пула соединений
            dataSource.setInitialSize(5);              // Начальное количество соединений
            dataSource.setMinIdle(5);                  // Минимальное количество простаивающих соединений
            dataSource.setMaxTotal(20);                // Максимальное количество соединений
            dataSource.setMaxWaitMillis(30000);        // Максимальное время ожидания соединения (30 сек)
            
            // Параметры жизненного цикла соединений
            dataSource.setMinEvictableIdleTimeMillis(600000);     // Минимальное время простоя перед удалением (10 мин)
            dataSource.setTimeBetweenEvictionRunsMillis(60000);   // Интервал проверки простаивающих соединений (1 мин)
            
            // Параметры валидации соединений
            dataSource.setTestOnBorrow(true);          // Проверять соединение при получении из пула
            dataSource.setTestWhileIdle(true);         // Проверять простаивающие соединения
            dataSource.setValidationQuery("SELECT 1"); // SQL запрос для проверки соединения
            dataSource.setValidationQueryTimeout(3);   // Таймаут валидационного запроса (3 сек)
            
            // Prepared Statements пул
            dataSource.setPoolPreparedStatements(true);      // Включить пул prepared statements
            dataSource.setMaxOpenPreparedStatements(50);     // Максимум prepared statements
            
            // Параметры для обнаружения "брошенных" соединений
            dataSource.setRemoveAbandonedOnBorrow(true);     // Удалять брошенные соединения при получении
            dataSource.setRemoveAbandonedTimeout(300);       // Таймаут для брошенных соединений (5 мин)
            dataSource.setLogAbandoned(true);                // Логировать брошенные соединения
            
            // Параметры транзакций
            dataSource.setDefaultAutoCommit(false);          // Отключить автокоммит
            dataSource.setDefaultTransactionIsolation(       // Уровень изоляции READ_COMMITTED
                java.sql.Connection.TRANSACTION_READ_COMMITTED
            );
            
            // Форматированный лог инициализации
            String initMessage = String.format("""
                    ========================================
                    ✓ APACHE COMMONS DBCP2 INITIALIZED
                    ========================================
                    DataSource Class: %s
                    Pool Configuration:
                      - initialSize: %d
                      - minIdle: %d
                      - maxTotal: %d
                    Timeout Configuration:
                      - maxWaitMillis: %d ms
                      - minEvictableIdleTimeMillis: %d ms
                      - timeBetweenEvictionRunsMillis: %d ms
                    Validation Configuration:
                      - testOnBorrow: %s
                      - testWhileIdle: %s
                      - validationQuery: %s
                    ========================================
                    """,
                    dataSource.getClass().getName(),
                    dataSource.getInitialSize(),
                    dataSource.getMinIdle(),
                    dataSource.getMaxTotal(),
                    dataSource.getMaxWaitMillis(),
                    dataSource.getMinEvictableIdleTimeMillis(),
                    dataSource.getTimeBetweenEvictionRunsMillis(),
                    dataSource.getTestOnBorrow(),
                    dataSource.getTestWhileIdle(),
                    dataSource.getValidationQuery()
            );
            logger.info(initMessage);
        }
        return dataSource;
    }
    
    /**
     * Логирует статистику пула соединений
     */
    public void logPoolStatistics() {
        if (dataSource != null) {
            String stats = String.format("""
                    === DBCP2 Pool Statistics ===
                    Active connections: %d
                    Idle connections: %d
                    Total connections: %d
                    Max total: %d
                    =============================
                    """,
                    dataSource.getNumActive(),
                    dataSource.getNumIdle(),
                    dataSource.getNumActive() + dataSource.getNumIdle(),
                    dataSource.getMaxTotal()
            );
            logger.info(stats);
        }
    }
    
    /**
     * Закрывает DataSource при остановке приложения
     */
    @PreDestroy
    public void closeDataSource() {
        if (dataSource != null) {
            try {
                String closeMessage = String.format("""
                        Closing DBCP2 DataSource...
                        Final pool stats:
                          - Active connections: %d
                          - Idle connections: %d
                        ✓ DataSource closed successfully
                        """,
                        dataSource.getNumActive(),
                        dataSource.getNumIdle()
                );
                logger.info(closeMessage);
                dataSource.close();
            } catch (SQLException e) {
                logger.severe("✗ Error closing DataSource: " + e.getMessage());
            }
        }
    }
}
