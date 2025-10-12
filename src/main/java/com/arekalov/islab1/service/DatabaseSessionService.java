package com.arekalov.islab1.service;

import org.eclipse.persistence.sessions.DatabaseSession;
import org.eclipse.persistence.sessions.DatabaseLogin;
import org.eclipse.persistence.sessions.Project;
import org.eclipse.persistence.platform.database.PostgreSQLPlatform;
import org.eclipse.persistence.sequencing.NativeSequence;
import org.eclipse.persistence.logging.SessionLog;

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
    
    private DatabaseSession databaseSession;
    
    @PostConstruct
    public void init() {
        try {
            logger.info("Инициализация EclipseLink DatabaseSession...");
            
            // Получаем DataSource из JNDI
            InitialContext ctx = new InitialContext();
            DataSource dataSource = (DataSource) ctx.lookup("java:jboss/datasources/flatsPu");
            
            // Создаем DatabaseLogin
            DatabaseLogin login = new DatabaseLogin();
            login.useExternalConnectionPooling();
            login.setConnector(new org.eclipse.persistence.sessions.JNDIConnector(dataSource));
            login.usePlatform(new PostgreSQLPlatform());
            
            // Настройки для отключения транзакций (по требованию)
            login.setUsesExternalTransactionController(false);
            login.setShouldBindAllParameters(false);
            login.setUsesJDBCBatchWriting(false);
            
            // Создаем проект с дескрипторами через утилитарный класс
            Project project = new Project();
            project.setLogin(login);
            
            // Используем DescriptorBuilder для создания всех дескрипторов
            project.addDescriptor(DescriptorBuilder.buildHouseDescriptor());
            project.addDescriptor(DescriptorBuilder.buildCoordinatesDescriptor());
            project.addDescriptor(DescriptorBuilder.buildFlatDescriptor());
            
            // Создаем и настраиваем сессию
            databaseSession = project.createDatabaseSession();
            databaseSession.setLogLevel(SessionLog.INFO);
            
            // Настраиваем последовательности для автогенерации ID (PostgreSQL SERIAL)
            setupSequences(databaseSession);
            
            // Логинимся в сессию
            databaseSession.login();
            
            logger.info("EclipseLink DatabaseSession успешно инициализирован");
            
        } catch (Exception e) {
            logger.severe("Ошибка инициализации EclipseLink DatabaseSession: " + e.getMessage());
            throw new RuntimeException("Failed to initialize EclipseLink DatabaseSession", e);
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
    public DatabaseSession getDatabaseSession() {
        if (databaseSession == null || !databaseSession.isConnected()) {
            throw new RuntimeException("DatabaseSession не инициализирован или отключен");
        }
        return databaseSession;
    }
    
    /**
     * Проверить, активна ли сессия
     */
    public boolean isSessionActive() {
        return databaseSession != null && databaseSession.isConnected();
    }
    
    @PreDestroy
    public void cleanup() {
        if (databaseSession != null && databaseSession.isConnected()) {
            logger.info("Закрытие EclipseLink DatabaseSession...");
            try {
                databaseSession.logout();
                logger.info("EclipseLink DatabaseSession успешно закрыт");
            } catch (Exception e) {
                logger.warning("Ошибка при закрытии DatabaseSession: " + e.getMessage());
            }
        }
    }
}
