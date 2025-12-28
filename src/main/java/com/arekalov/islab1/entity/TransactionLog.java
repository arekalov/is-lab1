package com.arekalov.islab1.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Журнал распределенных транзакций для двухфазного коммита (2PC)
 * 
 * Отслеживает состояние транзакций между PostgreSQL и MinIO
 * для обеспечения согласованности с использованием staging/final областей
 */
@Entity
@Table(name = "transaction_log")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionLog {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * Глобальный идентификатор транзакции (UUID)
     */
    @Column(name = "transaction_id", unique = true, nullable = false)
    private String transactionId;
    
    /**
     * Состояние транзакции в протоколе 2PC
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private TransactionState state;
    
    /**
     * Путь к файлу в staging области MinIO (неподтвержденный)
     * Формат: staging/{uuid}.json
     */
    @Column(name = "staging_object_key")
    private String stagingObjectKey;
    
    /**
     * Путь к файлу в final области MinIO (подтвержденный)
     * Формат: final/{uuid}.json
     */
    @Column(name = "final_object_key")
    private String finalObjectKey;
    
    /**
     * ID записи ImportHistory (заполняется после успешного коммита)
     */
    @Column(name = "import_history_id")
    private Long importHistoryId;
    
    /**
     * Оригинальное имя файла
     */
    @Column(name = "file_name")
    private String fileName;
    
    /**
     * Размер файла в байтах
     */
    @Column(name = "file_size")
    private Long fileSize;
    
    /**
     * SHA-256 хэш содержимого файла для обеспечения идемпотентности
     */
    @Column(name = "file_hash", length = 64)
    private String fileHash;
    
    /**
     * Время создания записи
     */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    /**
     * Время последнего обновления
     */
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    /**
     * Время истечения транзакции (для обнаружения зависших)
     */
    @Column(name = "timeout_at")
    private LocalDateTime timeoutAt;
    
    /**
     * Сериализованные операции импорта (JSON)
     * Сохраняются в фазе PREPARE для выполнения в фазе COMMIT
     */
    @Column(name = "validated_operations", columnDefinition = "TEXT")
    private String validatedOperations;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (timeoutAt == null) {
            // По умолчанию транзакция истекает через 10 минут
            timeoutAt = LocalDateTime.now().plusMinutes(10);
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    /**
     * Состояния транзакции в 2PC протоколе
     */
    public enum TransactionState {
        /**
         * Подготовка началась (файл загружается в staging)
         */
        PREPARING,
        
        /**
         * Оба участника готовы к коммиту:
         * - MinIO: файл в staging/{uuid}.json
         * - Database: данные провалидированы
         * Это точка "голосования" - оба vote: COMMIT
         */
        PREPARED,
        
        /**
         * Коммит начался:
         * - Database: операции выполняются
         * - MinIO: файл копируется staging -> final
         */
        COMMITTING,
        
        /**
         * Транзакция успешно завершена:
         * - Database: ImportHistory создан
         * - MinIO: файл в final/{uuid}.json, staging удален
         */
        COMMITTED,
        
        /**
         * Откат начался (один из участников vote: ABORT)
         */
        ABORTING,
        
        /**
         * Транзакция откачена:
         * - Database: rollback
         * - MinIO: staging файл удален
         */
        ABORTED
    }
}

