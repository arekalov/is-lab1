-- Добавление поля validated_operations в transaction_log
-- Для хранения провалидированных операций между фазами PREPARE и COMMIT

ALTER TABLE transaction_log 
ADD COLUMN IF NOT EXISTS validated_operations TEXT;

COMMENT ON COLUMN transaction_log.validated_operations IS 'Сериализованные операции импорта (JSON), провалидированные в фазе PREPARE';

