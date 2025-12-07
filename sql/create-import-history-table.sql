-- Таблица истории операций импорта
-- Хранит информацию ТОЛЬКО об УСПЕШНЫХ операциях массового добавления объектов

CREATE TABLE IF NOT EXISTS import_history (
    id BIGSERIAL PRIMARY KEY,
    operation_time TIMESTAMP NOT NULL,
    objects_count INTEGER NOT NULL CHECK (objects_count > 0),
    changes_description TEXT NOT NULL
);

-- Индекс для оптимизации запросов по времени (сортировка)
CREATE INDEX IF NOT EXISTS idx_import_history_operation_time ON import_history(operation_time DESC);

-- GIN индекс для эффективного поиска по JSON
CREATE INDEX IF NOT EXISTS idx_import_history_changes_gin ON import_history USING GIN (changes_description);

-- Комментарии для документации
COMMENT ON TABLE import_history IS 'История УСПЕШНЫХ операций массового импорта объектов';
COMMENT ON COLUMN import_history.id IS 'Уникальный идентификатор операции импорта';
COMMENT ON COLUMN import_history.operation_time IS 'Время выполнения операции';
COMMENT ON COLUMN import_history.objects_count IS 'Количество успешно импортированных объектов';
COMMENT ON COLUMN import_history.changes_description IS 'Детальное описание изменений в формате JSONB. Массив объектов: [{"type": "FLAT", "id": 1, "name": "..."}, ...]';


