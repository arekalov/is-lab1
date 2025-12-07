package com.arekalov.islab1.repository;

import com.arekalov.islab1.entity.ImportHistory;
import com.arekalov.islab1.service.EntityManagerService;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import java.util.List;
import java.util.logging.Logger;

/**
 * Репозиторий для работы с историей импорта
 */
@Stateless
public class ImportHistoryRepository {
    
    private static final Logger logger = Logger.getLogger(ImportHistoryRepository.class.getName());
    
    @Inject
    private EntityManagerService emService;
    
    /**
     * Сохранить запись об импорте
     */
    public ImportHistory save(ImportHistory importHistory) {
        logger.info("ImportHistoryRepository.save() - сохранение записи импорта");
        
        EntityManager em = emService.getEntityManager();
        
        if (importHistory.getId() == null) {
            em.persist(importHistory);
            logger.info("ImportHistoryRepository.save() - запись создана, id=" + importHistory.getId());
        } else {
            importHistory = em.merge(importHistory);
            logger.info("ImportHistoryRepository.save() - запись обновлена, id=" + importHistory.getId());
        }
        
        return importHistory;
    }
    
    /**
     * Найти запись по ID
     */
    public ImportHistory findById(Long id) {
        logger.info("ImportHistoryRepository.findById() - поиск записи id=" + id);
        
        EntityManager em = emService.getEntityManager();
        return em.find(ImportHistory.class, id);
    }
    
    /**
     * Получить все записи с пагинацией
     */
    public List<ImportHistory> findAll(int page, int size) {
        logger.info("ImportHistoryRepository.findAll() - страница " + page + ", размер " + size);
        
        EntityManager em = emService.getEntityManager();
        
        TypedQuery<ImportHistory> query = em.createQuery(
            "SELECT ih FROM ImportHistory ih ORDER BY ih.operationTime DESC",
            ImportHistory.class
        );
        
        query.setFirstResult(page * size);
        query.setMaxResults(size);
        
        return query.getResultList();
    }
    
    /**
     * Подсчитать общее количество записей
     */
    public long count() {
        logger.info("ImportHistoryRepository.count() - подсчет записей");
        
        EntityManager em = emService.getEntityManager();
        
        return em.createQuery(
            "SELECT COUNT(ih) FROM ImportHistory ih",
            Long.class
        ).getSingleResult();
    }
    
    /**
     * Получить последние N записей
     */
    public List<ImportHistory> findLatest(int limit) {
        logger.info("ImportHistoryRepository.findLatest() - последние " + limit + " записей");
        
        EntityManager em = emService.getEntityManager();
        
        TypedQuery<ImportHistory> query = em.createQuery(
            "SELECT ih FROM ImportHistory ih ORDER BY ih.operationTime DESC",
            ImportHistory.class
        );
        
        query.setMaxResults(limit);
        
        return query.getResultList();
    }
}

