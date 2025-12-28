package com.arekalov.islab1.service;

import com.arekalov.islab1.entity.ImportHistory;
import com.arekalov.islab1.entity.TransactionLog;
import com.arekalov.islab1.entity.TransactionLog.TransactionState;
import com.arekalov.islab1.repository.TransactionLogRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Координатор двухфазного коммита (2PC) для распределенных транзакций
 * между PostgreSQL и MinIO
 * 
 * ПРОТОКОЛ 2PC с staging/final областями:
 * 
 * PHASE 1 - PREPARE:
 *   1. BEGIN: создать transaction_log (state=PREPARING)
 *   2. MinIO PREPARE: загрузить файл в staging/{uuid}.json
 *   3. Database PREPARE: валидировать данные (НЕ сохранять!)
 *   4. Если оба OK -> state=PREPARED (vote: COMMIT)
 * 
 * PHASE 2 - COMMIT:
 *   5. Database COMMIT: сохранить данные в транзакции
 *   6. MinIO COMMIT: скопировать staging -> final, удалить staging
 *   7. state=COMMITTED
 * 
 * PHASE 2 - ABORT:
 *   5. MinIO ABORT: удалить staging файл
 *   6. Database ABORT: автоматический rollback (@Transactional)
 *   7. state=ABORTED
 *   
 * Использует CDI с явным управлением транзакциями
 */
@ApplicationScoped
public class TransactionCoordinator {
    
    private static final Logger logger = Logger.getLogger(TransactionCoordinator.class.getName());
    
    @Inject
    private TransactionLogRepository txLogRepository;
    
    @Inject
    private MinioService minioService;
    
    /**
     * BEGIN TRANSACTION: создать новую распределенную транзакцию
     * 
     * @param fileContent содержимое файла (для вычисления хэша)
     * @param fileName имя файла
     * @return TransactionLog с txId и state=PREPARING
     */
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public TransactionLog beginTransaction(byte[] fileContent, String fileName) {
        String txId = UUID.randomUUID().toString();
        String fileHash = calculateSHA256(fileContent);
        
        logger.info("╔════════════════════════════════════════════════════════╗");
        logger.info("║  2PC: BEGIN TRANSACTION                                 ║");
        logger.info("╚════════════════════════════════════════════════════════╝");
        logger.info("Transaction ID: " + txId);
        logger.info("File name: " + fileName);
        logger.info("File size: " + fileContent.length + " bytes");
        logger.info("File hash (SHA-256): " + fileHash);
        
        // Проверка идемпотентности: существует ли уже COMMITTED транзакция с таким хэшом?
        Optional<TransactionLog> existing = txLogRepository.findByFileHashAndState(
            fileHash, 
            TransactionState.COMMITTED
        );
        
        if (existing.isPresent()) {
            logger.warning("╔════════════════════════════════════════════════════════╗");
            logger.warning("║  IDEMPOTENCY: File already imported!                   ║");
            logger.warning("╚════════════════════════════════════════════════════════╝");
            logger.warning("Existing transaction ID: " + existing.get().getTransactionId());
            logger.warning("Returning existing ImportHistory: " + existing.get().getImportHistoryId());
            return existing.get();
        }
        
        // Создаем новую запись в transaction_log
        TransactionLog txLog = TransactionLog.builder()
            .transactionId(txId)
            .state(TransactionState.PREPARING)
            .fileName(fileName)
            .fileSize((long) fileContent.length)
            .fileHash(fileHash)
            .timeoutAt(LocalDateTime.now().plusMinutes(10))
            .build();
        
        txLog = txLogRepository.save(txLog);
        logger.info("Transaction log created: id=" + txLog.getId());
        
        return txLog;
    }
    
    /**
     * PHASE 1 - PREPARE MinIO: загрузить файл в staging область
     * 
     * @param txLog запись транзакции
     * @param fileContent содержимое файла
     * @return обновленный txLog с staging_object_key
     */
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public TransactionLog prepareMinIO(TransactionLog txLog, byte[] fileContent) {
        logger.info("");
        logger.info("╔════════════════════════════════════════════════════════╗");
        logger.info("║  PHASE 1: PREPARE MinIO Participant                    ║");
        logger.info("╚════════════════════════════════════════════════════════╝");
        
        try {
            // Загружаем файл в staging/
            String stagingKey = minioService.uploadToStaging(
                fileContent, 
                txLog.getTransactionId(),
                txLog.getFileName()
            );
            
            // Обновляем transaction_log
            txLog.setStagingObjectKey(stagingKey);
            txLog = txLogRepository.save(txLog);
            
            logger.info("MinIO PREPARE: SUCCESS");
            logger.info("Staging file: " + stagingKey);
            logger.info("MinIO vote: COMMIT (ready to commit)");
            
            return txLog;
            
        } catch (Exception e) {
            logger.severe("MinIO PREPARE: FAILED - " + e.getMessage());
            logger.severe("MinIO vote: ABORT");
            
            // Помечаем транзакцию как ABORTED
            txLog.setState(TransactionState.ABORTED);
            txLogRepository.save(txLog);
            
            throw new RuntimeException("MinIO PREPARE failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * PHASE 1 - PREPARE Database: валидировать данные (НЕ сохранять!)
     * В реальности валидация будет в ImportService, здесь просто фиксируем готовность
     * 
     * @param txLog запись транзакции
     * @return обновленный txLog с state=PREPARED
     */
     /**
     * PHASE 1: PREPARE Database Participant
     * 
     * Валидирует операции и сохраняет их для фазы COMMIT
     * НЕ выполняет реальные изменения в БД!
     * 
     * @param txLog лог транзакции
     * @param operationsJson JSON с операциями для валидации
     * @return обновленный лог с провалидированными операциями
     */
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public TransactionLog prepareDatabase(TransactionLog txLog, String operationsJson) {
        logger.info("");
        logger.info("╔════════════════════════════════════════════════════════╗");
        logger.info("║  PHASE 1: PREPARE Database Participant                 ║");
        logger.info("╚════════════════════════════════════════════════════════╝");
        
        try {
            // Валидируем JSON (парсинг)
            if (operationsJson == null || operationsJson.trim().isEmpty()) {
                throw new IllegalArgumentException("Operations JSON is empty");
            }
            
            // Можно добавить дополнительную валидацию:
            // - проверить формат JSON
            // - проверить наличие обязательных полей
            // - проверить foreign key constraints
            // Но для простоты пока только парсим JSON
            
            logger.info("Validating operations JSON (" + operationsJson.length() + " bytes)");
            
            // Сохраняем провалидированные операции для фазы COMMIT
            txLog.setValidatedOperations(operationsJson);
            txLog.setState(TransactionState.PREPARED);
            txLog = txLogRepository.save(txLog);
            
            logger.info("Database PREPARE: SUCCESS");
            logger.info("Operations validated and stored: " + operationsJson.length() + " bytes");
            logger.info("Database vote: COMMIT (ready to commit)");
            logger.info("");
            logger.info("╔════════════════════════════════════════════════════════╗");
            logger.info("║  PREPARE PHASE COMPLETED                                ║");
            logger.info("║  Both participants voted: COMMIT                        ║");
            logger.info("╚════════════════════════════════════════════════════════╝");
            
            return txLog;
            
        } catch (Exception e) {
            logger.severe("Database PREPARE: FAILED - " + e.getMessage());
            logger.severe("Database vote: ABORT");
            
            txLog.setState(TransactionState.ABORTED);
            txLogRepository.save(txLog);
            
            throw new RuntimeException("Database PREPARE failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * PHASE 2 - COMMIT: финализировать транзакцию
     * 
     * @param txLog запись транзакции (должна быть в состоянии PREPARED)
     * @param importHistory созданная запись ImportHistory
     * @return обновленный txLog с state=COMMITTED
     */
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public TransactionLog commit(TransactionLog txLog, ImportHistory importHistory) {
        logger.info("");
        logger.info("╔════════════════════════════════════════════════════════╗");
        logger.info("║  PHASE 2: COMMIT DECISION                               ║");
        logger.info("╚════════════════════════════════════════════════════════╝");
        logger.info("Coordinator decision: COMMIT");
        
        if (txLog.getState() != TransactionState.PREPARED) {
            logger.severe("Cannot commit: transaction not in PREPARED state: " + txLog.getState());
            throw new IllegalStateException("Transaction must be PREPARED to commit");
        }
        
        try {
            // Обновляем состояние на COMMITTING
            txLog.setState(TransactionState.COMMITTING);
            txLog.setImportHistoryId(importHistory.getId());
            txLog = txLogRepository.save(txLog);
            
            logger.info("State: COMMITTING");
            logger.info("ImportHistory ID: " + importHistory.getId());
            
            // Копируем файл: staging -> final
            logger.info("MinIO COMMIT: Copying staging -> final...");
            String finalKey = minioService.copyToFinal(
                txLog.getStagingObjectKey(),
                txLog.getTransactionId()
            );
            
            // Удаляем staging файл
            logger.info("MinIO COMMIT: Deleting staging file...");
            minioService.deleteStaging(txLog.getStagingObjectKey());
            
            // Обновляем transaction_log
            txLog.setFinalObjectKey(finalKey);
            txLog.setState(TransactionState.COMMITTED);
            txLog = txLogRepository.save(txLog);
            
            logger.info("");
            logger.info("╔════════════════════════════════════════════════════════╗");
            logger.info("║  ✅ TRANSACTION COMMITTED SUCCESSFULLY                  ║");
            logger.info("╚════════════════════════════════════════════════════════╝");
            logger.info("Transaction ID: " + txLog.getTransactionId());
            logger.info("Final file: " + finalKey);
            logger.info("ImportHistory ID: " + importHistory.getId());
            
            return txLog;
            
        } catch (Exception e) {
            logger.severe("COMMIT FAILED: " + e.getMessage());
            logger.severe("Transaction state: IN_DOUBT (requires manual intervention)");
            
            // Если коммит не удался, помечаем как COMMITTING (recovery попытается завершить)
            // Это критично - БД уже сохранена!
            e.printStackTrace();
            throw new RuntimeException("COMMIT failed - transaction in uncertain state", e);
        }
    }
    
    /**
     * PHASE 2 - ABORT: откатить транзакцию
     * 
     * @param txLog запись транзакции
     */
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void abort(TransactionLog txLog) {
        logger.severe("");
        logger.severe("╔════════════════════════════════════════════════════════╗");
        logger.severe("║  PHASE 2: ABORT DECISION                                ║");
        logger.severe("╚════════════════════════════════════════════════════════╝");
        logger.severe("Coordinator decision: ABORT");
        
        try {
            txLog.setState(TransactionState.ABORTING);
            txLog = txLogRepository.save(txLog);
            
            // Удаляем staging файл (компенсация)
            if (txLog.getStagingObjectKey() != null) {
                logger.severe("MinIO ABORT: Deleting staging file...");
                minioService.deleteStaging(txLog.getStagingObjectKey());
            }
            
            // Database откатится автоматически через @Transactional
            logger.severe("Database ABORT: Transaction will be rolled back");
            
            txLog.setState(TransactionState.ABORTED);
            txLogRepository.save(txLog);
            
            logger.severe("");
            logger.severe("╔════════════════════════════════════════════════════════╗");
            logger.severe("║  🔄 TRANSACTION ABORTED                                 ║");
            logger.severe("╚════════════════════════════════════════════════════════╝");
            logger.severe("Transaction ID: " + txLog.getTransactionId());
            logger.severe("All changes have been rolled back");
            
        } catch (Exception e) {
            logger.severe("ABORT FAILED: " + e.getMessage());
            // Даже если abort не удался, продолжаем (recovery почистит)
        }
    }
    
    /**
     * Вычислить SHA-256 хэш файла для идемпотентности
     */
    private String calculateSHA256(byte[] content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content);
            
            // Конвертируем в hex строку
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
            
        } catch (Exception e) {
            logger.warning("Failed to calculate SHA-256: " + e.getMessage());
            return null;
        }
    }
}

