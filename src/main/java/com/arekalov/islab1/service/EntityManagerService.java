package com.arekalov.islab1.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.logging.Logger;

/**
 * Сервис для управления EntityManager
 * Предоставляет доступ к EntityManager для репозиториев
 */
@ApplicationScoped
public class EntityManagerService {
    
    private static final Logger logger = Logger.getLogger(EntityManagerService.class.getName());
    
    @PersistenceContext(unitName = "flatsPU")
    private EntityManager entityManager;
    
    /**
     * Получить EntityManager для использования в репозиториях
     */
    public EntityManager getEntityManager() {
        if (entityManager == null) {
            logger.severe("EntityManager is null! Check persistence.xml configuration");
            throw new IllegalStateException("EntityManager is not available");
        }
        return entityManager;
    }
    
    /**
     * Начать транзакцию с указанным уровнем изоляции
     */
    public void beginTransactionWithIsolation(int isolationLevel) {
        EntityManager em = getEntityManager();
        if (!em.getTransaction().isActive()) {
            em.getTransaction().begin();
            // Устанавливаем уровень изоляции через нативный SQL
            em.createNativeQuery("SET TRANSACTION ISOLATION LEVEL " + getIsolationLevelName(isolationLevel))
                .executeUpdate();
        }
    }
    
    /**
     * Получить название уровня изоляции
     */
    private String getIsolationLevelName(int isolationLevel) {
        switch (isolationLevel) {
            case java.sql.Connection.TRANSACTION_READ_UNCOMMITTED:
                return "READ UNCOMMITTED";
            case java.sql.Connection.TRANSACTION_READ_COMMITTED:
                return "READ COMMITTED";
            case java.sql.Connection.TRANSACTION_REPEATABLE_READ:
                return "REPEATABLE READ";
            case java.sql.Connection.TRANSACTION_SERIALIZABLE:
                return "SERIALIZABLE";
            default:
                return "READ COMMITTED";
        }
    }
    
    /**
     * Проверить, активна ли транзакция
     */
    public boolean isTransactionActive() {
        return entityManager != null && entityManager.getTransaction().isActive();
    }
}

