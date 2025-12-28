package com.arekalov.islab1.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import java.util.logging.Logger;

/**
 * Сервис для управления EntityManager с DBCP2
 * Создает EntityManager из нашего кастомного EMF
 */
@ApplicationScoped
public class EntityManagerService {
    
    private static final Logger logger = Logger.getLogger(EntityManagerService.class.getName());
    
    @Inject
    private EntityManagerFactory emf;
    
    private ThreadLocal<EntityManager> entityManagerThreadLocal = new ThreadLocal<>();
    
    /**
     * Получить EntityManager для использования в репозиториях
     */
    public EntityManager getEntityManager() {
        EntityManager em = entityManagerThreadLocal.get();
        if (em == null || !em.isOpen()) {
            em = emf.createEntityManager();
            entityManagerThreadLocal.set(em);
            logger.fine("Created new EntityManager for thread: " + Thread.currentThread().getName());
        }
        return em;
    }
    
    /**
     * Закрыть EntityManager после использования
     */
    public void closeEntityManager() {
        EntityManager em = entityManagerThreadLocal.get();
        if (em != null && em.isOpen()) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            em.close();
            entityManagerThreadLocal.remove();
        }
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
        EntityManager em = entityManagerThreadLocal.get();
        return em != null && em.isOpen() && em.getTransaction().isActive();
    }
}



