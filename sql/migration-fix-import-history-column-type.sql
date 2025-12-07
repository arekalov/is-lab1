-- Миграция: изменение типа колонки changes_description с JSONB на TEXT
-- Дата: 2025-12-07

DO $$
BEGIN
    -- Проверяем, существует ли таблица и колонка
    IF EXISTS (
        SELECT 1 
        FROM information_schema.columns 
        WHERE table_name = 'import_history' 
        AND column_name = 'changes_description'
    ) THEN
        -- Изменяем тип колонки с JSONB на TEXT
        ALTER TABLE import_history 
        ALTER COLUMN changes_description TYPE TEXT 
        USING changes_description::TEXT;
        
        RAISE NOTICE 'Column "changes_description" type changed from JSONB to TEXT.';
    ELSE
        RAISE NOTICE 'Column "changes_description" does not exist. Skipping migration.';
    END IF;
END $$;

COMMENT ON COLUMN import_history.changes_description IS 'JSON-строка с описанием изменений (созданные/обновленные/удаленные объекты)';

