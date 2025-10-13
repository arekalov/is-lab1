package com.arekalov.islab1.service;

import org.eclipse.persistence.sessions.DatabaseSession;
import org.eclipse.persistence.sessions.DatabaseLogin;
import org.eclipse.persistence.sessions.Project;
import org.eclipse.persistence.platform.database.PostgreSQLPlatform;
import org.eclipse.persistence.sequencing.NativeSequence;
import org.eclipse.persistence.logging.SessionLog;
import org.eclipse.persistence.queries.DatabaseQuery;

import com.arekalov.islab1.repository.mapping.DescriptorBuilder;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import javax.naming.InitialContext;
import javax.sql.DataSource;
import java.util.logging.Logger;

/**
 * Сервис для управления EclipseLink DatabaseSession
 * Централизованная инициализация и настройка сессии БД
 */
@ApplicationScoped
public class DatabaseSessionService {
    
    private static final Logger logger = Logger.getLogger(DatabaseSessionService.class.getName());
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final long RETRY_DELAY_MS = 1000; // 1 секунда между попытками
    
    private DatabaseSession databaseSession;
    private DataSource dataSource;
    private Project project;
    
    @PostConstruct
    public void init() {
        try {
            logger.info("Инициализация EclipseLink DatabaseSession...");
            
            // Получаем DataSource из JNDI
            InitialContext ctx = new InitialContext();
            dataSource = (DataSource) ctx.lookup("java:jboss/datasources/flatsPu");
            
            // Создаем проект с дескрипторами
            project = createProject();
            
            // Создаем сессию с повторными попытками
            createAndInitializeSession();
            
            logger.info("EclipseLink DatabaseSession успешно инициализирован");
            
        } catch (Exception e) {
            logger.severe("Ошибка инициализации EclipseLink DatabaseSession: " + e.getMessage());
            throw new RuntimeException("Failed to initialize EclipseLink DatabaseSession", e);
        }
    }
    
    private Project createProject() {
        // Создаем DatabaseLogin
        DatabaseLogin login = new DatabaseLogin();
        login.useExternalConnectionPooling();
        login.setConnector(new org.eclipse.persistence.sessions.JNDIConnector(dataSource));
        login.usePlatform(new PostgreSQLPlatform());
        
        // Настройки для отключения транзакций (по требованию)
        login.setUsesExternalTransactionController(false);
        login.setShouldBindAllParameters(false);
        login.setUsesJDBCBatchWriting(false);
        
        // Настройки соединения
        login.setTransactionIsolation(java.sql.Connection.TRANSACTION_READ_COMMITTED);
        
        // Дополнительные настройки через свойства
        java.util.Properties properties = new java.util.Properties();
        // Отключаем кэширование
        properties.setProperty("eclipselink.cache.shared.default", "false");
        properties.setProperty("eclipselink.cache.size.default", "0");
        properties.setProperty("eclipselink.cache.type.default", "None");
        // Всегда читаем актуальные данные из базы
        properties.setProperty("eclipselink.refresh", "true");
        
        // Настройки соединений
        properties.setProperty("eclipselink.jdbc.connections.initial", "1");
        properties.setProperty("eclipselink.jdbc.connections.min", "1");
        properties.setProperty("eclipselink.jdbc.connections.max", "5");
        properties.setProperty("eclipselink.jdbc.read-connections.min", "1");
        properties.setProperty("eclipselink.jdbc.write-connections.min", "1");
        properties.setProperty("eclipselink.jdbc.timeout", "10");
        
        // Расширенное логирование
        properties.setProperty("eclipselink.logging.level", "FINE");
        properties.setProperty("eclipselink.logging.connection", "true");
        properties.setProperty("eclipselink.logging.exceptions", "true");
        properties.setProperty("eclipselink.logging.sql", "FINE");
        login.setProperties(properties);
        
        // Создаем проект с дескрипторами
        Project project = new Project();
        project.setLogin(login);
        
        // Добавляем дескрипторы
        project.addDescriptor(DescriptorBuilder.buildHouseDescriptor());
        project.addDescriptor(DescriptorBuilder.buildCoordinatesDescriptor());
        project.addDescriptor(DescriptorBuilder.buildFlatDescriptor());
        
        return project;
    }
    
    private synchronized void createAndInitializeSession() {
        int attempts = 0;
        Exception lastException = null;
        
        while (attempts < MAX_RETRY_ATTEMPTS) {
            try {
                // Clean up old session if exists
                cleanupOldSession();
                
                // Create new session with exponential backoff
                long currentDelay = RETRY_DELAY_MS * (long)Math.pow(2, attempts);
                if (attempts > 0) {
                    logger.info("Waiting " + currentDelay + "ms before retry " + (attempts + 1));
                    Thread.sleep(currentDelay);
                }
                
                // Create and configure new session
                databaseSession = project.createDatabaseSession();
                databaseSession.setLogLevel(SessionLog.INFO);
                
                // Configure session event listeners for connection monitoring
                configureSessionEventListeners(databaseSession);
                
                // Setup sequences before login
                setupSequences(databaseSession);
                
                // Login with timeout
                try {
                    databaseSession.login();
                } catch (Exception e) {
                    logger.warning("Login failed: " + e.getMessage());
                    throw e;
                }
                
                // Verify connection with transaction test
                if (testConnection()) {
                    logger.info("Session successfully created and verified on attempt " + (attempts + 1));
                    return;
                } else {
                    throw new RuntimeException("Connection test failed after login");
                }
                
            } catch (Exception e) {
                lastException = e;
                attempts++;
                
                logger.warning("Session creation attempt " + attempts + "/" + MAX_RETRY_ATTEMPTS + 
                             " failed: " + e.getMessage());
                
                if (attempts >= MAX_RETRY_ATTEMPTS) {
                    break;
                }
                
                // Clean up failed session
                cleanupOldSession();
            }
        }
        
        String errorMsg = "Failed to create session after " + MAX_RETRY_ATTEMPTS + " attempts";
        logger.severe(errorMsg);
        throw new RuntimeException(errorMsg, lastException);
    }
    
    private void cleanupOldSession() {
        if (databaseSession != null) {
            try {
                if (databaseSession.isConnected()) {
                    // Rollback any pending transaction
                    if (databaseSession.isInTransaction()) {
                        try {
                            databaseSession.rollbackTransaction();
                        } catch (Exception e) {
                            logger.warning("Failed to rollback transaction: " + e.getMessage());
                        }
                    }
                    
                    // Logout from session
                    databaseSession.logout();
                }
            } catch (Exception e) {
                logger.warning("Error cleaning up old session: " + e.getMessage());
            } finally {
                databaseSession = null;
            }
        }
    }
    
    private void configureSessionEventListeners(DatabaseSession session) {
        // Запускаем периодическую проверку соединения
        java.util.Timer connectionTimer = new java.util.Timer("ConnectionHealthCheck", true);
        connectionTimer.scheduleAtFixedRate(new java.util.TimerTask() {
            @Override
            public void run() {
                try {
                    if (!isSessionActive()) {
                        logger.warning("Обнаружено неактивное соединение, попытка восстановления...");
                        createAndInitializeSession();
                    }
                } catch (Exception e) {
                    logger.severe("Ошибка при проверке состояния соединения: " + e.getMessage());
                }
            }
        }, 30000, 30000); // Проверка каждые 30 секунд
        
        // Добавляем shutdown hook для корректного завершения таймера
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            connectionTimer.cancel();
        }));
    }
    
    private boolean testConnection() {
        if (databaseSession == null) {
            return false;
        }

        try {
            // Start a transaction to ensure connection is valid
            databaseSession.beginTransaction();
            try {
                // Test both read and write capabilities
                databaseSession.executeSQL("SELECT 1");
                return true;
            } finally {
                // Always rollback test transaction
                if (databaseSession.isInTransaction()) {
                    databaseSession.rollbackTransaction();
                }
            }
        } catch (Exception e) {
            logger.warning("Тест подключения не пройден: " + e.getMessage());
            // Try to cleanup any hanging transaction
            try {
                if (databaseSession.isInTransaction()) {
                    databaseSession.rollbackTransaction();
                }
            } catch (Exception ignored) {
                // Ignore cleanup errors
            }
            return false;
        }
    }
    
    /**
     * Настройка последовательностей для автогенерации ID
     */
    private void setupSequences(DatabaseSession session) {
        logger.info("Настройка PostgreSQL последовательностей...");
        
        NativeSequence houseSequence = new NativeSequence("houses_id_seq", 1);
        NativeSequence coordinatesSequence = new NativeSequence("coordinates_id_seq", 1);
        NativeSequence flatSequence = new NativeSequence("flats_id_seq", 1);
        
        session.getLogin().addSequence(houseSequence);
        session.getLogin().addSequence(coordinatesSequence);
        session.getLogin().addSequence(flatSequence);
        
        logger.info("PostgreSQL последовательности настроены");
    }
    
    /**
     * Получить DatabaseSession для использования в репозиториях
     */
    public synchronized DatabaseSession getDatabaseSession() {
        if (!isSessionActive()) {
            logger.warning("Сессия неактивна, пробуем пересоздать...");
            createAndInitializeSession();
        }
        return databaseSession;
    }
    
    /**
     * Проверить, активна ли сессия
     */
    public boolean isSessionActive() {
        if (databaseSession == null) {
            return false;
        }
        
        try {
            return databaseSession.isConnected() && testConnection();
        } catch (Exception e) {
            logger.warning("Ошибка при проверке состояния сессии: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Безопасное выполнение запроса с автоматическим восстановлением соединения
     */
    public synchronized Object executeQuery(DatabaseQuery query) {
        int attempts = 0;
        Exception lastException = null;
        
        while (attempts < MAX_RETRY_ATTEMPTS) {
            try {
                if (!isSessionActive()) {
                    createAndInitializeSession();
                }
                return databaseSession.executeQuery(query);
            } catch (Exception e) {
                lastException = e;
                attempts++;
                logger.warning("Попытка " + attempts + " выполнения запроса не удалась: " + e.getMessage());
                
                if (attempts < MAX_RETRY_ATTEMPTS) {
                    try {
                        Thread.sleep(RETRY_DELAY_MS);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
        
        throw new RuntimeException("Не удалось выполнить запрос после " + MAX_RETRY_ATTEMPTS + 
                                 " попыток", lastException);
    }
    
    @PreDestroy
    public void cleanup() {
        if (databaseSession != null) {
            logger.info("Закрытие EclipseLink DatabaseSession...");
            try {
                if (databaseSession.isConnected()) {
                    databaseSession.logout();
                }
                databaseSession = null;
                logger.info("EclipseLink DatabaseSession успешно закрыт");
            } catch (Exception e) {
                logger.warning("Ошибка при закрытии DatabaseSession: " + e.getMessage());
            }
        }
    }
}