-- Таблица для координатора двухфазного коммита (2PC)
-- Отслеживает состояние распределенных транзакций между PostgreSQL и MinIO

CREATE TABLE IF NOT EXISTS transaction_log (
    id BIGSERIAL PRIMARY KEY,
    
    -- Глобальный идентификатор транзакции
    transaction_id VARCHAR(255) UNIQUE NOT NULL,
    
    -- Состояние транзакции в протоколе 2PC
    -- PREPARING: подготовка началась
    -- PREPARED: оба участника готовы (staging файл загружен, данные валидны)
    -- COMMITTING: коммит начался (БД сохраняется, файл копируется в final)
    -- COMMITTED: успешно завершено (БД сохранена, файл в final/)
    -- ABORTING: откат начался
    -- ABORTED: успешно откачено (staging файл удален)
    state VARCHAR(50) NOT NULL,
    
    -- MinIO файловые пути
    staging_object_key VARCHAR(255),          -- staging/{uuid}.json
    final_object_key VARCHAR(255),            -- final/{uuid}.json
    
    -- Import metadata
    import_history_id BIGINT,                 -- FK к import_history (после коммита)
    file_name VARCHAR(255),                   -- Оригинальное имя файла
    file_size BIGINT,                         -- Размер файла в байтах
    file_hash VARCHAR(64),                    -- SHA-256 хэш для идемпотентности
    
    -- Timestamps
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    timeout_at TIMESTAMP,                     -- Время истечения транзакции (для recovery)
    
    -- Constraint
    CONSTRAINT check_state CHECK (state IN ('PREPARING', 'PREPARED', 'COMMITTING', 'COMMITTED', 'ABORTING', 'ABORTED'))
);

-- Индекс для быстрого поиска по transaction_id
CREATE INDEX idx_transaction_log_tx_id ON transaction_log(transaction_id);

-- Индекс для поиска зависших транзакций (требующих recovery)
CREATE INDEX idx_transaction_log_timeout ON transaction_log(state, timeout_at) 
WHERE state IN ('PREPARING', 'PREPARED', 'COMMITTING', 'ABORTING');

-- Индекс для поиска по хэшу файла (идемпотентность)
CREATE INDEX idx_transaction_log_file_hash ON transaction_log(file_hash, state)
WHERE state = 'COMMITTED';

-- Индекс для поиска по staging_object_key (cleanup)
CREATE INDEX idx_transaction_log_staging_key ON transaction_log(staging_object_key)
WHERE staging_object_key IS NOT NULL;

-- Комментарии
COMMENT ON TABLE transaction_log IS 'Журнал двухфазных транзакций для координации PostgreSQL и MinIO';
COMMENT ON COLUMN transaction_log.transaction_id IS 'Глобальный идентификатор транзакции (UUID)';
COMMENT ON COLUMN transaction_log.state IS 'Состояние транзакции: PREPARING → PREPARED → COMMITTING → COMMITTED';
COMMENT ON COLUMN transaction_log.staging_object_key IS 'Путь к файлу в staging области (неподтвержденный)';
COMMENT ON COLUMN transaction_log.final_object_key IS 'Путь к файлу в final области (подтвержденный)';
COMMENT ON COLUMN transaction_log.file_hash IS 'SHA-256 хэш файла для обеспечения идемпотентности';
COMMENT ON COLUMN transaction_log.timeout_at IS 'Время, после которого транзакция считается зависшей';

