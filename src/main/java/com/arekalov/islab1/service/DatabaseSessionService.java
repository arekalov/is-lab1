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
                if (databaseSession != null) {
                    try {
                        databaseSession.logout();
                    } catch (Exception e) {
                        logger.warning("Ошибка при закрытии старой сессии: " + e.getMessage());
                    }
                }
                
                // Создаем новую сессию
                databaseSession = project.createDatabaseSession();
                databaseSession.setLogLevel(SessionLog.INFO);
                
                // Настраиваем последовательности
                setupSequences(databaseSession);
                
                // Логинимся в сессию
                databaseSession.login();
                
                // Проверяем подключение
                if (testConnection()) {
                    logger.info("Сессия успешно создана и проверена");
                    return;
                }
                
            } catch (Exception e) {
                lastException = e;
                attempts++;
                logger.warning("Попытка " + attempts + " из " + MAX_RETRY_ATTEMPTS + 
                             " создания сессии не удалась: " + e.getMessage());
                
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
        
        throw new RuntimeException("Не удалось создать сессию после " + MAX_RETRY_ATTEMPTS + 
                                 " попыток", lastException);
    }
    
    private boolean testConnection() {
        try {
            // Пробуем выполнить простой запрос для проверки соединения
            databaseSession.executeSQL("SELECT 1");
            return true;
        } catch (Exception e) {
            logger.warning("Тест подключения не пройден: " + e.getMessage());
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