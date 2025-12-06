-- Миграция: Добавление поля floor в таблицу flats
-- Дата: 2025-12-06
-- Описание: Добавляет обязательное поле floor (этаж) со значением по умолчанию 1 для существующих записей

-- Шаг 1: Добавляем колонку floor (без NOT NULL пока)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 
        FROM information_schema.columns 
        WHERE table_name = 'flats' 
          AND column_name = 'floor'
    ) THEN
        ALTER TABLE flats ADD COLUMN floor INTEGER;
        RAISE NOTICE 'Колонка floor добавлена';
    ELSE
        RAISE NOTICE 'Колонка floor уже существует';
    END IF;
END $$;

-- Шаг 2: Устанавливаем значение 1 для всех существующих записей
UPDATE flats SET floor = 1 WHERE floor IS NULL;

-- Шаг 3: Делаем колонку обязательной (NOT NULL)
ALTER TABLE flats ALTER COLUMN floor SET NOT NULL;

-- Шаг 4: Добавляем ограничение CHECK (floor > 0)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 
        FROM information_schema.constraint_column_usage 
        WHERE constraint_name = 'flats_floor_check'
    ) THEN
        ALTER TABLE flats ADD CONSTRAINT flats_floor_check CHECK (floor > 0);
        RAISE NOTICE 'Ограничение flats_floor_check добавлено';
    ELSE
        RAISE NOTICE 'Ограничение flats_floor_check уже существует';
    END IF;
END $$;

-- Шаг 5 (опционально): Создаем индексы для производительности
CREATE INDEX IF NOT EXISTS idx_flats_house_floor ON flats(house_id, floor);
CREATE INDEX IF NOT EXISTS idx_flats_coordinates_floor ON flats(coordinates_id, floor);

-- Проверка результата
SELECT 
    column_name, 
    data_type, 
    is_nullable, 
    column_default
FROM information_schema.columns 
WHERE table_name = 'flats' 
  AND column_name = 'floor';

SELECT 'Миграция успешно завершена! Добавлена колонка floor.' AS result;

