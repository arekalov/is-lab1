package com.arekalov.islab1.service;

import com.arekalov.islab1.entity.TransactionLog;
import com.arekalov.islab1.entity.TransactionLog.TransactionState;
import com.arekalov.islab1.repository.ImportHistoryRepository;
import com.arekalov.islab1.repository.TransactionLogRepository;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.*;
import jakarta.inject.Inject;

import java.util.List;
import java.util.logging.Logger;

/**
 * Recovery Manager для восстановления зависших распределенных транзакций
 * 
 * Автоматически запускается по расписанию и проверяет:
 * 1. Транзакции с истекшим timeout
 * 2. Транзакции в промежуточных состояниях (PREPARING, COMMITTING, ABORTING)
 * 3. Осиротевшие staging файлы в MinIO
 * 
 * Стратегия recovery:
 * - PREPARING/PREPARED с timeout -> ABORT (безопасно откатить)
 * - COMMITTING с timeout -> COMMIT (завершить коммит, БД уже сохранена!)
 * - ABORTING с timeout -> ABORT (завершить откат)
 */
@Singleton
@Startup
public class TransactionRecoveryService {
    
    private static final Logger logger = Logger.getLogger(TransactionRecoveryService.class.getName());
    
    @Inject
    private TransactionLogRepository txLogRepository;
    
    @Inject
    private TransactionCoordinator txCoordinator;
    
    @Inject
    private MinioService minioService;
    
    @Inject
    private ImportHistoryRepository importHistoryRepository;
    
    @PostConstruct
    public void init() {
        logger.info("TransactionRecoveryService initialized");
        logger.info("Recovery job will run every 5 minutes");
        logger.info("Cleanup job will run daily at 3:00 AM");
    }
    
    /**
     * Периодическая проверка и восстановление зависших транзакций
     * Запускается каждые 5 минут
     */
    @Schedule(minute = "*/5", hour = "*", persistent = false)
    public void recoverTimedOutTransactions() {
        logger.info("╔════════════════════════════════════════════════════════╗");
        logger.info("║  Transaction Recovery Job Started                       ║");
        logger.info("╚════════════════════════════════════════════════════════╝");
        
        try {
            List<TransactionLog> timedOutLogs = txLogRepository.findTimedOutTransactions();
            
            if (timedOutLogs.isEmpty()) {
                logger.info("No timed-out transactions found");
                return;
            }
            
            logger.warning("Found " + timedOutLogs.size() + " timed-out transactions");
            
            for (TransactionLog txLog : timedOutLogs) {
                try {
                    recoverTransaction(txLog);
                } catch (Exception e) {
                    logger.severe("Failed to recover transaction " + txLog.getTransactionId() + ": " + e.getMessage());
                }
            }
                
        } catch (Exception e) {
            logger.severe("Recovery job failed: " + e.getMessage());
            e.printStackTrace();
        }
        
        logger.info("╔════════════════════════════════════════════════════════╗");
        logger.info("║  Transaction Recovery Job Completed                      ║");
        logger.info("╚════════════════════════════════════════════════════════╝");
    }
    
    /**
     * Восстановление конкретной транзакции
     */
    private void recoverTransaction(TransactionLog txLog) {
        logger.warning("╔════════════════════════════════════════════════════════╗");
        logger.warning("║  RECOVERY: " + txLog.getTransactionId());
        logger.warning("╚════════════════════════════════════════════════════════╝");
        logger.warning("State: " + txLog.getState());
        logger.warning("Created: " + txLog.getCreatedAt());
        logger.warning("Timeout: " + txLog.getTimeoutAt());
        
        try {
            switch (txLog.getState()) {
                case PREPARING:
                case PREPARED:
                    // Безопасно откатить - данных в БД еще нет
                    logger.warning("Strategy: ABORT (transaction was preparing, no DB changes yet)");
                    txCoordinator.abort(txLog);
                    logger.info("Recovery SUCCESS: Transaction aborted");
                    break;
                    
                case COMMITTING:
                    // КРИТИЧНО! БД уже сохранена, нужно завершить коммит MinIO
                    logger.warning("Strategy: COMMIT (transaction was committing, DB already saved!)");
                    
                    // Проверяем, существует ли final файл
                    if (txLog.getFinalObjectKey() != null && minioService.finalFileExists(txLog.getFinalObjectKey())) {
                        logger.info("Final file already exists, just updating state");
                        txLog.setState(TransactionState.COMMITTED);
                        txLogRepository.save(txLog);
                    } else {
                        // Завершаем копирование staging -> final
                        var importHistory = importHistoryRepository.findById(txLog.getImportHistoryId());
                        txCoordinator.commit(txLog, importHistory);
                    }
                    logger.info("Recovery SUCCESS: Transaction committed");
                    break;
                    
                case ABORTING:
                    // Завершить откат
                    logger.warning("Strategy: ABORT (transaction was aborting)");
                    txCoordinator.abort(txLog);
                    logger.info("Recovery SUCCESS: Transaction aborted");
                    break;
                    
                default:
                    logger.warning("Unknown state for recovery: " + txLog.getState());
            }
            
        } catch (Exception e) {
            logger.severe("Recovery FAILED for transaction " + txLog.getTransactionId() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Очистка старых завершенных транзакций и осиротевших staging файлов
     * Запускается раз в день в 3:00
     */
    @Schedule(hour = "3", minute = "0", persistent = false)
    public void cleanupOldData() {
        logger.info("╔════════════════════════════════════════════════════════╗");
        logger.info("║  Cleanup Job Started                                    ║");
        logger.info("╚════════════════════════════════════════════════════════╝");
        
        try {
            // Удаляем транзакции старше 30 дней
            int deleted = txLogRepository.deleteOldCompletedTransactions(30);
            logger.info("Deleted " + deleted + " old transaction log entries (older than 30 days)");
            
            // Очищаем staging файлы старше 24 часов
            cleanupOrphanedStagingFiles();
            
        } catch (Exception e) {
            logger.severe("Cleanup job failed: " + e.getMessage());
            e.printStackTrace();
        }
        
        logger.info("╔════════════════════════════════════════════════════════╗");
        logger.info("║  Cleanup Job Completed                                   ║");
        logger.info("╚════════════════════════════════════════════════════════╝");
    }
    
    /**
     * Очистка осиротевших staging файлов
     */
    private void cleanupOrphanedStagingFiles() {
        logger.info("Cleaning up orphaned staging files...");
        
        try {
            // Находим все staging файлы в MinIO
            List<String> stagingFiles = minioService.listStagingFiles();
            logger.info("Found " + stagingFiles.size() + " staging files in MinIO");
            
            int deletedCount = 0;
            for (String stagingKey : stagingFiles) {
                try {
                    // Проверяем, есть ли активная транзакция для этого файла
                    var txLogOpt = txLogRepository.findByStagingObjectKey(stagingKey);
                    
                    if (txLogOpt.isEmpty()) {
                        // Осиротевший файл - транзакция не найдена
                        logger.warning("Orphaned staging file (no transaction): " + stagingKey);
                        minioService.deleteStaging(stagingKey);
                        deletedCount++;
                    } else {
                        TransactionLog txLog = txLogOpt.get();
                        if (txLog.getState() == TransactionState.ABORTED || 
                            txLog.getState() == TransactionState.COMMITTED) {
                            // Файл от завершенной транзакции
                            logger.warning("Orphaned staging file (tx finished): " + stagingKey);
                            minioService.deleteStaging(stagingKey);
                            deletedCount++;
                        }
                    }
                } catch (Exception e) {
                    logger.warning("Failed to cleanup staging file " + stagingKey + ": " + e.getMessage());
                }
            }
            
            logger.info("Deleted " + deletedCount + " orphaned staging files");
            
        } catch (Exception e) {
            logger.severe("Failed to cleanup staging files: " + e.getMessage());
        }
    }
    
    /**
     * Проверка и отчет о состоянии транзакций (запускается каждый час)
     */
    @Schedule(hour = "*", minute = "0", persistent = false)
    public void reportTransactionStatus() {
        try {
            long activeCount = txLogRepository.countActiveTransactions();
            
            if (activeCount > 0) {
                logger.info("Active transactions count: " + activeCount);
            }
            
            // Проверяем PREPARING/COMMITTING транзакции (подозрительные если долго)
            List<TransactionLog> preparingLogs = txLogRepository.findByState(TransactionState.PREPARING);
            List<TransactionLog> committingLogs = txLogRepository.findByState(TransactionState.COMMITTING);
            
            if (!preparingLogs.isEmpty()) {
                logger.warning("Transactions in PREPARING state: " + preparingLogs.size());
            }
            if (!committingLogs.isEmpty()) {
                logger.warning("Transactions in COMMITTING state: " + committingLogs.size());
            }
            
        } catch (Exception e) {
            logger.severe("Status report failed: " + e.getMessage());
        }
    }
    
    /**
     * Ручной триггер recovery для конкретной транзакции
     * (может быть вызван через JMX или REST API)
     */
    public void manualRecover(String transactionId) {
        logger.info("Manual recovery triggered for transaction: " + transactionId);
        
        var txLogOpt = txLogRepository.findByTransactionId(transactionId);
        if (txLogOpt.isPresent()) {
            recoverTransaction(txLogOpt.get());
        } else {
            logger.warning("Transaction not found: " + transactionId);
        }
    }
}

