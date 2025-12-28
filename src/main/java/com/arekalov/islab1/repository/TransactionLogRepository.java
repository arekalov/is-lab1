package com.arekalov.islab1.repository;

import com.arekalov.islab1.entity.TransactionLog;
import com.arekalov.islab1.entity.TransactionLog.TransactionState;
import com.arekalov.islab1.service.EntityManagerService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.transaction.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository для работы с журналом транзакций (2PC)
 * 
 * Использует CDI (@ApplicationScoped) с явным управлением транзакциями (@Transactional)
 */
@ApplicationScoped
public class TransactionLogRepository {
    
    @Inject
    private EntityManagerService emService;
    
    /**
     * Сохранить запись в журнале транзакций
     * Использует REQUIRED - присоединяется к существующей транзакции или создает новую
     */
    @Transactional(Transactional.TxType.REQUIRED)
    public TransactionLog save(TransactionLog log) {
        EntityManager em = emService.getEntityManager();
        
        if (log.getId() == null) {
            em.persist(log);
            return log;
        } else {
            return em.merge(log);
        }
    }
    
    /**
     * Найти запись по ID
     * Поддерживает существующую транзакцию для консистентности чтения
     */
    @Transactional(Transactional.TxType.SUPPORTS)
    public Optional<TransactionLog> findById(Long id) {
        EntityManager em = emService.getEntityManager();
        TransactionLog log = em.find(TransactionLog.class, id);
        return Optional.ofNullable(log);
    }
    
    /**
     * Найти запись по ID транзакции
     */
    @Transactional(Transactional.TxType.SUPPORTS)
    public Optional<TransactionLog> findByTransactionId(String transactionId) {
        EntityManager em = emService.getEntityManager();
        TypedQuery<TransactionLog> query = em.createQuery(
            "SELECT t FROM TransactionLog t WHERE t.transactionId = :txId",
            TransactionLog.class
        );
        query.setParameter("txId", transactionId);
        
        List<TransactionLog> results = query.getResultList();
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }
    
    /**
     * Найти существующую COMMITTED транзакцию по хэшу файла (для идемпотентности)
     */
    @Transactional(Transactional.TxType.SUPPORTS)
    public Optional<TransactionLog> findByFileHashAndState(String fileHash, TransactionState state) {
        EntityManager em = emService.getEntityManager();
        TypedQuery<TransactionLog> query = em.createQuery(
            "SELECT t FROM TransactionLog t WHERE t.fileHash = :hash AND t.state = :state",
            TransactionLog.class
        );
        query.setParameter("hash", fileHash);
        query.setParameter("state", state);
        
        List<TransactionLog> results = query.getResultList();
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }
    
    /**
     * Найти запись по staging object key
     */
    @Transactional(Transactional.TxType.SUPPORTS)
    public Optional<TransactionLog> findByStagingObjectKey(String stagingObjectKey) {
        EntityManager em = emService.getEntityManager();
        TypedQuery<TransactionLog> query = em.createQuery(
            "SELECT t FROM TransactionLog t WHERE t.stagingObjectKey = :key",
            TransactionLog.class
        );
        query.setParameter("key", stagingObjectKey);
        
        List<TransactionLog> results = query.getResultList();
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }
    
    /**
     * Найти все зависшие транзакции (timeout истек)
     * для recovery механизма
     */
    @Transactional(Transactional.TxType.SUPPORTS)
    public List<TransactionLog> findTimedOutTransactions() {
        EntityManager em = emService.getEntityManager();
        TypedQuery<TransactionLog> query = em.createQuery(
            "SELECT t FROM TransactionLog t " +
            "WHERE t.state IN :states " +
            "AND t.timeoutAt < :now " +
            "ORDER BY t.createdAt",
            TransactionLog.class
        );
        query.setParameter("states", List.of(
            TransactionState.PREPARING,
            TransactionState.PREPARED,
            TransactionState.COMMITTING,
            TransactionState.ABORTING
        ));
        query.setParameter("now", LocalDateTime.now());
        return query.getResultList();
    }
    
    /**
     * Найти все транзакции в указанном состоянии
     */
    @Transactional(Transactional.TxType.SUPPORTS)
    public List<TransactionLog> findByState(TransactionState state) {
        EntityManager em = emService.getEntityManager();
        TypedQuery<TransactionLog> query = em.createQuery(
            "SELECT t FROM TransactionLog t WHERE t.state = :state ORDER BY t.createdAt",
            TransactionLog.class
        );
        query.setParameter("state", state);
        return query.getResultList();
    }
    
    /**
     * Удалить старые завершенные транзакции (для очистки)
     * Удаляет записи старше указанного количества дней
     * REQUIRES_NEW - выполняется в отдельной транзакции (cleanup операция)
     */
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public int deleteOldCompletedTransactions(int olderThanDays) {
        EntityManager em = emService.getEntityManager();
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(olderThanDays);
        return em.createQuery(
            "DELETE FROM TransactionLog t " +
            "WHERE t.state IN :completedStates " +
            "AND t.updatedAt < :cutoffDate"
        )
        .setParameter("completedStates", List.of(
            TransactionState.COMMITTED,
            TransactionState.ABORTED
        ))
        .setParameter("cutoffDate", cutoffDate)
        .executeUpdate();
    }
    
    /**
     * Получить количество активных транзакций
     */
    @Transactional(Transactional.TxType.SUPPORTS)
    public long countActiveTransactions() {
        EntityManager em = emService.getEntityManager();
        TypedQuery<Long> query = em.createQuery(
            "SELECT COUNT(t) FROM TransactionLog t " +
            "WHERE t.state NOT IN :finalStates",
            Long.class
        );
        query.setParameter("finalStates", List.of(
            TransactionState.COMMITTED,
            TransactionState.ABORTED
        ));
        return query.getSingleResult();
    }
    
    /**
     * Найти все staging файлы старше указанного времени
     * (для cleanup операций)
     */
    @Transactional(Transactional.TxType.SUPPORTS)
    public List<TransactionLog> findOldStagingFiles(int hoursAgo) {
        EntityManager em = emService.getEntityManager();
        LocalDateTime cutoffDate = LocalDateTime.now().minusHours(hoursAgo);
        TypedQuery<TransactionLog> query = em.createQuery(
            "SELECT t FROM TransactionLog t " +
            "WHERE t.stagingObjectKey IS NOT NULL " +
            "AND t.state IN :staleStates " +
            "AND t.updatedAt < :cutoffDate " +
            "ORDER BY t.createdAt",
            TransactionLog.class
        );
        query.setParameter("staleStates", List.of(
            TransactionState.PREPARING,
            TransactionState.PREPARED,
            TransactionState.ABORTED
        ));
        query.setParameter("cutoffDate", cutoffDate);
        return query.getResultList();
    }
}

