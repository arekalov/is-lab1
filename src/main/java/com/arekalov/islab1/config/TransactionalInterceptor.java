package com.arekalov.islab1.config;

import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import com.arekalov.islab1.service.EntityManagerService;

import java.util.logging.Logger;

/**
 * Interceptor для эмуляции @Transactional в RESOURCE_LOCAL режиме
 * Автоматически начинает и коммитит транзакции для методов с @Transactional
 */
@Transactional
@Interceptor
@Priority(Interceptor.Priority.APPLICATION)
public class TransactionalInterceptor {
    
    private static final Logger logger = Logger.getLogger(TransactionalInterceptor.class.getName());
    
    @Inject
    private EntityManagerService entityManagerService;
    
    @AroundInvoke
    public Object manageTransaction(InvocationContext context) throws Exception {
        EntityManager em = entityManagerService.getEntityManager();
        boolean isNewTransaction = false;
        
        try {
            // Начинаем транзакцию, если она еще не активна
            if (!em.getTransaction().isActive()) {
                em.getTransaction().begin();
                isNewTransaction = true;
                logger.fine("Started new transaction for " + context.getMethod().getName());
            }
            
            // Выполняем метод
            Object result = context.proceed();
            
            // Коммитим транзакцию, если мы ее создали
            if (isNewTransaction && em.getTransaction().isActive()) {
                em.getTransaction().commit();
                logger.fine("Committed transaction for " + context.getMethod().getName());
            }
            
            return result;
            
        } catch (Exception e) {
            // Откатываем транзакцию при ошибке
            if (isNewTransaction && em.getTransaction() != null && em.getTransaction().isActive()) {
                em.getTransaction().rollback();
                logger.warning("Rolled back transaction for " + context.getMethod().getName() + ": " + e.getMessage());
            }
            throw e;
        } finally {
            // Закрываем EntityManager после завершения транзакции
            if (isNewTransaction) {
                entityManagerService.closeEntityManager();
            }
        }
    }
}

