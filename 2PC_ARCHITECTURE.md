# Архитектура двухфазной фиксации (2PC) для Import Service

## 📋 Содержание
1. [Обзор](#обзор)
2. [Статусы транзакций](#статусы-транзакций)
3. [Диаграмма переходов состояний](#диаграмма-переходов-состояний)
4. [Ключевые компоненты](#ключевые-компоненты)
5. [Построчный разбор логики импорта](#построчный-разбор-логики-импорта)
6. [Протокол 2PC: краткая схема](#протокол-2pc-краткая-схема)
7. [Пример реального выполнения](#пример-реального-выполнения)
8. [Идемпотентность](#идемпотентность)
9. [Recovery механизм](#recovery-механизм)
10. [Структура классов](#структура-классов)
11. [Сценарии отказов](#сценарии-отказов)
12. [Преимущества и недостатки](#преимущества-этого-подхода)
13. [Резюме](#резюме)

---

## Обзор

Реализация **эмуляции 2PC** на уровне бизнес-логики для обеспечения согласованности между PostgreSQL и MinIO.

**Ключевая идея**: Разделить операцию на 2 фазы:
- **Phase 1 (PREPARE)**: Подготовить, но НЕ фиксировать изменения
- **Phase 2 (COMMIT/ABORT)**: Зафиксировать ИЛИ откатить все изменения

---

## Статусы транзакций

### Все возможные статусы

| Статус | Описание | Фаза 2PC | Финальный? |
|--------|----------|----------|-----------|
| `PREPARING` | Инициализация транзакции | Phase 1 | ❌ |
| `PREPARED` | Все участники готовы | Phase 1 | ❌ |
| `COMMITTING` | Фиксация изменений начата | Phase 2 | ❌ |
| `COMMITTED` | Все изменения зафиксированы | Phase 2 | ✅ |
| `ABORTING` | Откат изменений начат | Phase 2 | ❌ |
| `ABORTED` | Все изменения откачены | Phase 2 | ✅ |

### Детальное описание каждого статуса

#### 1. **PREPARING** (начальное состояние)

**Когда устанавливается:**
- Сразу после создания записи в `transaction_log`
- До загрузки файла в MinIO

**Что происходит:**
```java
TransactionLog txLog = TransactionLog.builder()
    .transactionId(UUID.randomUUID().toString())
    .state(TransactionState.PREPARING)
    .fileName(fileName)
    .fileSize(fileContent.length)
    .createdAt(LocalDateTime.now())
    .timeoutAt(LocalDateTime.now().plusMinutes(10))
    .build();
```

**Состояние ресурсов:**
- ❌ MinIO staging: файл НЕ загружен
- ❌ MinIO final: пусто
- ❌ БД: данные НЕ сохранены
- ✅ transaction_log: запись создана

**Следующий статус:**
- → `PREPARED` (если подготовка успешна)
- → `ABORTED` (если ошибка на этапе подготовки)

---

#### 2. **PREPARED** (готовность участников)

**Когда устанавливается:**
- После успешной загрузки файла в MinIO staging
- После успешной валидации JSON

**Что происходит:**
```java
// 1. Загрузили файл в staging
minioService.uploadToStaging(txId, fileContent);

// 2. Провалидировали JSON
List<ImportOperationRequest> operations = validateJson(json);

// 3. Обновили статус
txLog.setState(TransactionState.PREPARED);
txLog.setStagingObjectKey("staging/" + txId + ".json");
txLog.setValidatedOperations(json);
```

**Состояние ресурсов:**
- ✅ MinIO staging: файл загружен (`staging/{txId}.json`)
- ❌ MinIO final: пусто
- ❌ БД: данные НЕ сохранены (только валидированы)
- ✅ transaction_log: `state=PREPARED`, `staging_object_key` заполнен

**Следующий статус:**
- → `COMMITTING` (если координатор решил COMMIT)
- → `ABORTING` (если координатор решил ABORT)

**Критический момент**: Это точка принятия решения координатором!

---

#### 3. **COMMITTING** (фиксация начата)

**Когда устанавливается:**
- После начала сохранения данных в БД
- ПЕРЕД копированием файла из staging в final

**Что происходит:**
```java
// 1. Начинаем фиксацию
txLog.setState(TransactionState.COMMITTING);
txLogRepository.save(txLog);  // ⚠️ Важно: сохранить ДО операций с данными

// 2. Выполняем операции с данными
for (ImportOperationRequest operation : operations) {
    processFlatOperation(operation.getType(), operation.getData());
}

// 3. Создаем ImportHistory
ImportHistory history = ImportHistory.builder()
    .operationTime(LocalDateTime.now())
    .objectsCount(successCount)
    .build();
history = importHistoryRepository.save(history);

// 4. Связываем с транзакцией
txLog.setImportHistoryId(history.getId());
```

**Состояние ресурсов:**
- ✅ MinIO staging: файл есть (`staging/{txId}.json`)
- ❌ MinIO final: пусто (еще не скопировали!)
- ⚠️ БД: данные СОХРАНЕНЫ (Flat, House, Coordinates, ImportHistory)
- ✅ transaction_log: `state=COMMITTING`, `import_history_id` заполнен

**⚠️ ОПАСНАЯ ЗОНА**: Если сервер упадет здесь, то:
- Данные в БД уже есть
- Но файл еще не в final/
- Recovery должен завершить копирование!

**Следующий статус:**
- → `COMMITTED` (после успешного копирования в final)

---

#### 4. **COMMITTED** (успешная фиксация) ✅

**Когда устанавливается:**
- После успешного копирования файла из staging в final
- После удаления файла из staging

**Что происходит:**
```java
// 1. Копируем файл staging → final
String finalKey = "final/import-" + timestamp + ".json";
minioService.copyToFinal(stagingKey, finalKey);

// 2. Удаляем из staging (очистка)
minioService.deleteStaging(stagingKey);

// 3. Финальный статус
txLog.setState(TransactionState.COMMITTED);
txLog.setFinalObjectKey(finalKey);
txLog.setUpdatedAt(LocalDateTime.now());
txLogRepository.save(txLog);
```

**Состояние ресурсов:**
- ❌ MinIO staging: файл удален
- ✅ MinIO final: файл скопирован (`final/import-...json`)
- ✅ БД: все данные сохранены
- ✅ transaction_log: `state=COMMITTED`, `final_object_key` заполнен
- ✅ ImportHistory: запись доступна с `fileObjectKey`

**Это финальное успешное состояние!** Транзакция завершена. ✅

---

#### 5. **ABORTING** (откат начат)

**Когда устанавливается:**
- При любой ошибке валидации
- При нарушении бизнес-правил
- При ошибке сохранения данных

**Что происходит:**
```java
// 1. Отмечаем, что начинаем откат
txLog.setState(TransactionState.ABORTING);
txLogRepository.save(txLog);

// 2. Удаляем файл из staging
if (txLog.getStagingObjectKey() != null) {
    minioService.deleteStaging(txLog.getStagingObjectKey());
}

// 3. БД откатится автоматически через @Transactional
// (все INSERT/UPDATE/DELETE отменятся)

// 4. Финальный статус
txLog.setState(TransactionState.ABORTED);
txLog.setUpdatedAt(LocalDateTime.now());
```

**Состояние ресурсов:**
- ❌ MinIO staging: файл удаляется
- ❌ MinIO final: пусто
- ❌ БД: транзакция откатывается (ROLLBACK)
- ✅ transaction_log: `state=ABORTING`

**Следующий статус:**
- → `ABORTED` (после очистки staging и ROLLBACK БД)

---

#### 6. **ABORTED** (откат завершен) ❌

**Когда устанавливается:**
- После успешного удаления файла из staging
- После отката транзакции БД

**Что происходит:**
```java
// Финальное обновление
txLog.setState(TransactionState.ABORTED);
txLog.setUpdatedAt(LocalDateTime.now());
txLogRepository.save(txLog);

// HTTP Response
throw new UniqueConstraintViolationException("...");
// → 400 Bad Request для клиента
```

**Состояние ресурсов:**
- ❌ MinIO staging: файл удален
- ❌ MinIO final: пусто
- ❌ БД: все изменения откачены (как будто ничего не было)
- ✅ transaction_log: `state=ABORTED` (только запись об ошибке)
- ❌ ImportHistory: запись НЕ создана

**Это финальное состояние ошибки!** Все откачено. ❌

---

## Диаграмма переходов состояний

### Граф состояний (FSM - Finite State Machine)

```
                    [START]
                       ↓
                 ┌─────────────┐
                 │  PREPARING  │ ← Начальное состояние
                 └─────────────┘
                       ↓
          ┌────────────┴────────────┐
          ↓                         ↓
    [SUCCESS]                  [ERROR]
          ↓                         ↓
    ┌─────────────┐          ┌─────────────┐
    │  PREPARED   │          │  ABORTING   │
    └─────────────┘          └─────────────┘
          ↓                         ↓
    [Coordinator                    ↓
     Decision]              ┌─────────────┐
          ↓                 │   ABORTED   │ ✅ FINAL
   ┌──────┴──────┐          └─────────────┘
   ↓             ↓
[COMMIT]      [ABORT]
   ↓             ↓
┌─────────────┐ └─> ABORTING → ABORTED
│ COMMITTING  │
└─────────────┘
       ↓
  [Success]
       ↓
┌─────────────┐
│  COMMITTED  │ ✅ FINAL
└─────────────┘
```

### Happy Path (успешный путь)

```
PREPARING → PREPARED → COMMITTING → COMMITTED ✅
```

**Время выполнения**: ~500ms - 2s

### Error Path (путь с ошибкой)

```
PREPARING → PREPARED → ABORTING → ABORTED ❌
          ↓
         [или напрямую]
          ↓
      ABORTING → ABORTED ❌
```

**Время выполнения**: ~200ms - 1s (быстрее, т.к. нет записи в БД)

### Таблица переходов

| Из состояния | В состояние | Условие | Действие |
|-------------|------------|---------|----------|
| `PREPARING` | `PREPARED` | ✅ Файл загружен в staging + JSON валиден | Установить `staging_object_key` |
| `PREPARING` | `ABORTING` | ❌ Ошибка загрузки или валидации | Начать откат |
| `PREPARED` | `COMMITTING` | ✅ Координатор решил COMMIT | Начать сохранение в БД |
| `PREPARED` | `ABORTING` | ❌ Координатор решил ABORT | Удалить staging файл |
| `COMMITTING` | `COMMITTED` | ✅ Файл скопирован staging → final | Установить `final_object_key` |
| `COMMITTING` | `ABORTING` | ❌ Ошибка при копировании | Откат (редкий случай!) |
| `ABORTING` | `ABORTED` | ✅ Staging очищен + ROLLBACK | Финальное состояние ошибки |

---

## Ключевые компоненты

### 1. **Таблица transaction_log** (координатор транзакций)

```sql
CREATE TABLE transaction_log (
    id BIGSERIAL PRIMARY KEY,
    transaction_id VARCHAR(255) UNIQUE NOT NULL,  -- UUID транзакции
    state VARCHAR(50) NOT NULL,                   -- PREPARED, COMMITTED, ABORTED
    
    -- MinIO staging file info
    staging_object_key VARCHAR(255),              -- staging/{uuid}.json
    final_object_key VARCHAR(255),                -- final/{uuid}.json
    
    -- Import metadata
    import_history_id BIGINT,                     -- FK to import_history
    file_name VARCHAR(255),
    file_size BIGINT,
    
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    timeout_at TIMESTAMP,                          -- Для recovery (5-10 минут)
    
    CONSTRAINT check_state CHECK (state IN ('PREPARING', 'PREPARED', 'COMMITTING', 'COMMITTED', 'ABORTING', 'ABORTED'))
);
```

### 2. **MinIO структура бакетов**

```
import-files/
├── staging/          # Фаза PREPARE - файлы здесь НЕ "зафиксированы"
│   └── {uuid}.json   # Временные файлы
│
└── final/            # Фаза COMMIT - финальные файлы
    └── {uuid}.json   # Подтвержденные файлы
```

**Альтернатива**: один бакет с metadata тэгами:
- staging файл: metadata `status=pending`
- final файл: metadata `status=committed`

---

## Построчный разбор логики импорта

### 📍 Точка входа: HTTP Request

```http
POST /api/import
Content-Type: application/json

[{"type": "FLAT", "operation": "CREATE", "data": {...}}]
```

---

### 🎯 ImportController → ImportService

**Строка 73-75** (`ImportService.java`):
```java
public ImportHistory importObjects(String json) {
    return importObjectsWithFileName(json, "import-" + System.currentTimeMillis() + ".json");
}
```

**Что происходит:**
- Генерируем имя файла: `import-1703260728069.json`
- Вызываем основной метод `importObjectsWithFileName()`

---

### 🚀 ШАГ 1: BEGIN TRANSACTION

**Строка 88-95** (`ImportService.java`):
```java
@Transactional(Transactional.TxType.REQUIRED)  // ⚠️ JTA транзакция для БД
public ImportHistory importObjectsWithFileName(String json, String fileName) {
    byte[] fileContent = json.getBytes();
    TransactionLog txLog = null;
    
    try {
        txLog = txCoordinator.beginTransaction(fileContent, fileName);
```

#### TransactionCoordinator.beginTransaction()

**Строка 59-62** (`TransactionCoordinator.java`):
```java
@Transactional(Transactional.TxType.REQUIRES_NEW)  // ⚠️ НОВАЯ транзакция!
public TransactionLog beginTransaction(byte[] fileContent, String fileName) {
    String txId = UUID.randomUUID().toString();
    String fileHash = calculateSHA256(fileContent);
```

**Почему `REQUIRES_NEW`?**
- Создаем ОТДЕЛЬНУЮ транзакцию для `transaction_log`
- Если основная транзакция откатится, запись остается (аудит)

**Строка 72-85** — Проверка идемпотентности:
```java
Optional<TransactionLog> existing = txLogRepository.findByFileHashAndState(
    fileHash, TransactionState.COMMITTED
);

if (existing.isPresent()) {
    logger.warning("║  IDEMPOTENCY: File already imported!                   ║");
    return existing.get();  // ⚠️ EARLY RETURN!
}
```

**SQL запрос:**
```sql
SELECT * FROM transaction_log 
WHERE file_hash = '7a3b2f9c...' AND state = 'COMMITTED';
```

**Строка 87-98** — Создание записи:
```java
TransactionLog txLog = TransactionLog.builder()
    .transactionId(txId)
    .state(TransactionState.PREPARING)  // ← НАЧАЛЬНОЕ СОСТОЯНИЕ
    .fileName(fileName)
    .fileSize((long) fileContent.length)
    .fileHash(fileHash)
    .timeoutAt(LocalDateTime.now().plusMinutes(10))
    .build();

txLog = txLogRepository.save(txLog);
```

**SQL запрос:**
```sql
INSERT INTO transaction_log (
    transaction_id, state, file_name, file_size, file_hash, 
    created_at, timeout_at
) VALUES (
    'a3f7b2c9-...', 'PREPARING', 'import-...json', 458, 
    '7a3b2f9c...', NOW(), NOW() + INTERVAL '10 minutes'
);
```

**Состояние:**
- ✅ `transaction_log`: `state=PREPARING`
- ❌ MinIO staging: пусто
- ❌ БД (flats/houses): пусто

---

### 📍 ШАГ 2: PHASE 1 - PREPARE MinIO

**Строка 104-105** (`ImportService.java`):
```java
txLog = txCoordinator.prepareMinIO(txLog, fileContent);
```

#### TransactionCoordinator.prepareMinIO()

**Строка 117-123** (`TransactionCoordinator.java`):
```java
String stagingKey = minioService.uploadToStaging(
    fileContent, 
    txLog.getTransactionId(),
    txLog.getFileName()
);
```

**MinIO операция:**
```
PUT import-files/staging/a3f7b2c9-....json
Content: [{"type":"FLAT",...}]
```

**Строка 125-127** — Обновление txLog:
```java
txLog.setStagingObjectKey(stagingKey);
txLog = txLogRepository.save(txLog);
```

**SQL запрос:**
```sql
UPDATE transaction_log 
SET staging_object_key = 'staging/a3f7b2c9-...json',
    updated_at = NOW()
WHERE id = 85;
```

**Состояние после этого шага:**
- ✅ MinIO staging: `staging/a3f7b2c9-...json`
- ✅ `transaction_log`: `staging_object_key` заполнен
- ❌ MinIO final: пусто
- ❌ БД (flats/houses): пусто

---

### 📍 ШАГ 3: PHASE 1 - PREPARE Database

**Строка 107-108** (`ImportService.java`):
```java
txLog = txCoordinator.prepareDatabase(txLog, json);
```

#### TransactionCoordinator.prepareDatabase()

**Строка 171-175** (`TransactionCoordinator.java`):
```java
if (operationsJson == null || operationsJson.trim().isEmpty()) {
    throw new IllegalArgumentException("Operations JSON is empty");
}
```

**Строка 185-188** — Сохраняем JSON:
```java
txLog.setValidatedOperations(operationsJson);
txLog.setState(TransactionState.PREPARED);  // ← ОБА участника готовы!
txLog = txLogRepository.save(txLog);
```

**SQL запрос:**
```sql
UPDATE transaction_log 
SET validated_operations = '[{"type":"FLAT",...}]',
    state = 'PREPARED',         -- ← КРИТИЧЕСКИЙ ПЕРЕХОД!
    updated_at = NOW()
WHERE id = 85;
```

**Состояние:**
- ✅ MinIO staging: файл загружен
- ✅ `transaction_log`: `state=PREPARED` (голосование завершено!)
- ✅ `validated_operations`: JSON сохранен
- ❌ БД (flats/houses): ничего (еще НЕ сохраняли!)

---

### 🎯 КООРДИНАТОР: Решение COMMIT

**Строка 110-115** (`ImportService.java`):
```java
List<ImportOperationRequest> operations = objectMapper.readValue(
    txLog.getValidatedOperations(),  // ← Читаем сохраненный JSON
    objectMapper.getTypeFactory().constructCollectionType(...)
);

logger.info("║  PHASE 2: COMMIT Database Participant                   ║");
```

---

### 🔨 ШАГ 4: PHASE 2 - COMMIT Database

**Строка 121-151** — Цикл по операциям:
```java
int successCount = 0;
for (int i = 0; i < operations.size(); i++) {
    ImportOperationRequest operation = operations.get(i);
    
    int affectedObjects = 0;
    switch (type) {
        case "FLAT":
            affectedObjects = processFlatOperation(op, dataNode);  // ← СОЗДАЕМ!
            break;
        // ...
    }
    successCount += affectedObjects;
}
```

#### Внутри processFlatOperation() → createFlat():

```java
private int createFlat(JsonNode dataNode) {
    int createdObjects = 0;
    
    // 1. Создаем Coordinates
    coords = flatRepository.saveCoordinates(coords);  // INSERT INTO coordinates
    createdObjects++;
    
    // 2. Создаем или находим House
    if (houseNode.isNumber()) {
        house = houseRepository.findById(houseId);
    } else {
        house = houseRepository.save(house);  // INSERT INTO houses
        createdObjects++;
    }
    
    // 3. Валидируем с блокировкой!
    flatService.validateTerribleViewConstraint(flat);
    flatService.validateCoordinatesAndFloorUniqueness(flat);  // ← PESSIMISTIC LOCK!
    
    // 4. Создаем Flat
    flat = flatRepository.save(flat);  // INSERT INTO flats
    createdObjects++;
    
    return createdObjects;  // 3 (Coordinates + House + Flat)
}
```

**SQL запросы:**
```sql
-- 1. Координаты
INSERT INTO coordinates (x, y) VALUES (100, 200);  -- id=150

-- 2. Дом
INSERT INTO houses (name, year, number_of_flats_on_floor) 
VALUES ('Дом на Невском', 2020, 4);  -- id=100

-- 3. Блокировка дома
SELECT * FROM houses WHERE id = 100 FOR UPDATE;  -- 🔒 LOCK

-- 4. Проверка ограничений
SELECT COUNT(*) FROM flats f 
JOIN coordinates c ON f.coordinates_id = c.id
WHERE c.x = 100 AND c.y = 200 AND f.floor = 1;

-- 5. Квартира
INSERT INTO flats (name, floor, area, coordinates_id, house_id, ...) 
VALUES ('Квартира 101', 1, 50, 150, 100, ...);  -- id=201
```

**Строка 153-160** — Создание ImportHistory:
```java
ImportHistory history = ImportHistory.builder()
    .operationTime(LocalDateTime.now())
    .objectsCount(successCount)  // 3
    .build();

history = importHistoryRepository.save(history);
```

**SQL запрос:**
```sql
INSERT INTO import_history (operation_time, objects_count, created_at) 
VALUES (NOW(), 3, NOW());  -- id=123
```

**Состояние:**
- ✅ MinIO staging: файл есть
- ✅ БД: Coordinates, House, Flat, ImportHistory СОЗДАНЫ!
- ⚠️ Транзакция БД еще НЕ ЗАКОММИЧЕНА
- ❌ MinIO final: пусто

---

### 🚀 ШАГ 5: PHASE 2 - COMMIT MinIO

**Строка 162-163** (`ImportService.java`):
```java
txLog = txCoordinator.commit(txLog, history);
```

#### TransactionCoordinator.commit()

**Строка 233-239** (`TransactionCoordinator.java`):
```java
txLog.setState(TransactionState.COMMITTING);
txLog.setImportHistoryId(importHistory.getId());
txLog = txLogRepository.save(txLog);
```

**SQL запрос:**
```sql
UPDATE transaction_log 
SET state = 'COMMITTING',          -- ← КРИТИЧЕСКИЙ ПЕРЕХОД!
    import_history_id = 123,
    updated_at = NOW()
WHERE id = 85;
```

⚠️ **ОПАСНАЯ ЗОНА:** Если сервер упадет здесь, данные в БД есть, но файл не в final!

**Строка 241-246** — Копирование файла:
```java
String finalKey = minioService.copyToFinal(
    txLog.getStagingObjectKey(),
    txLog.getTransactionId()
);
```

**MinIO операции:**
```
COPY import-files/staging/a3f7b2c9-...json 
  TO import-files/final/import-2025-12-22-...json
```

**Строка 248-250** — Удаление staging:
```java
minioService.deleteStaging(txLog.getStagingObjectKey());
```

**MinIO операция:**
```
DELETE import-files/staging/a3f7b2c9-...json
```

**Строка 252-255** — Финальный статус:
```java
txLog.setFinalObjectKey(finalKey);
txLog.setState(TransactionState.COMMITTED);  // ← УСПЕХ!
txLog = txLogRepository.save(txLog);
```

**SQL запрос:**
```sql
UPDATE transaction_log 
SET state = 'COMMITTED',
    final_object_key = 'final/import-2025-12-22-...json',
    updated_at = NOW()
WHERE id = 85;
```

---

### 🔗 ШАГ 6: Связывание ImportHistory с файлом

**Строка 165-167** (`ImportService.java`):
```java
history.setFileObjectKey(txLog.getFinalObjectKey());
history = importHistoryRepository.save(history);
```

**SQL запрос:**
```sql
UPDATE import_history 
SET file_object_key = 'final/import-2025-12-22-...json'
WHERE id = 123;
```

---

### ✅ Возврат результата

**Строка 169**:
```java
return history;
```

**HTTP Response:**
```json
{
  "id": 123,
  "operationTime": "2025-12-22T12:38:48",
  "objectsCount": 3,
  "fileObjectKey": "final/import-2025-12-22-12-38-48-069.json"
}
```

**Финальное состояние:**

| Ресурс | Состояние |
|--------|-----------|
| **PostgreSQL** | Coordinates (150), House (100), Flat (201), ImportHistory (123) ✅ |
| **MinIO staging/** | Пусто (удален) |
| **MinIO final/** | `import-2025-12-22-...json` ✅ |
| **transaction_log** | `state=COMMITTED` ✅ |

---

### ❌ ОБРАБОТКА ОШИБОК

**Строка 171-184** (`ImportService.java`):
```java
} catch (ValidationException e) {
    abortTransaction(txLog);
    throw e;
    
} catch (UniqueConstraintViolationException e) {
    abortTransaction(txLog);
    throw e;
```

#### TransactionCoordinator.abort()

**Строка 292-299** (`TransactionCoordinator.java`):
```java
txLog.setState(TransactionState.ABORTING);
txLog = txLogRepository.save(txLog);

if (txLog.getStagingObjectKey() != null) {
    minioService.deleteStaging(txLog.getStagingObjectKey());
}
```

**MinIO операция:**
```
DELETE import-files/staging/a3f7b2c9-...json
```

**Строка 304-305**:
```java
txLog.setState(TransactionState.ABORTED);
txLogRepository.save(txLog);
```

**SQL запрос:**
```sql
ROLLBACK;  -- Все INSERT откатываются!

UPDATE transaction_log 
SET state = 'ABORTED', updated_at = NOW()
WHERE id = 85;
```

**Финальное состояние при ошибке:**
- ❌ PostgreSQL: Все откачено (ROLLBACK)
- ❌ MinIO staging: Пусто (удален)
- ❌ MinIO final: Пусто
- ✅ transaction_log: `state=ABORTED` (аудит)

**HTTP Response:** `400 Bad Request`

---

### 📊 Итоговая временная линия

```
t0:  HTTP POST /api/import
t1:  BEGIN TRANSACTION (state=PREPARING)
     → INSERT INTO transaction_log
t2:  PREPARE MinIO (upload to staging/)
     → PUT staging/a3f7b2c9-...json
t3:  PREPARE Database (validate JSON, state=PREPARED)
     → UPDATE transaction_log SET validated_operations
t4:  PHASE 2: COMMIT Database
     → INSERT INTO coordinates, houses, flats
t5:  CREATE ImportHistory
     → INSERT INTO import_history
t6:  UPDATE transaction_log (state=COMMITTING)
t7:  PHASE 2: COMMIT MinIO
     → COPY staging → final
t8:  DELETE staging file
t9:  UPDATE transaction_log (state=COMMITTED)
t10: UPDATE import_history (file_object_key)
t11: HTTP 200 OK
```

**Общее время:** ~800-1200ms

---

## Протокол 2PC: краткая схема

### PHASE 1: PREPARE (Подготовка)

```
┌─────────────────────────────────────────────────────────────┐
│ 1. BEGIN TRANSACTION (координатор)                          │
│    - txId = UUID.randomUUID()                               │
│    - INSERT INTO transaction_log (state=PREPARING)          │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│ 2. PREPARE MinIO (participant 1)                            │
│    - PUT staging/{txId}.json                                │
│    - UPDATE transaction_log: staging_object_key             │
│    - Если OK → vote: COMMIT                                 │
│    - Если ошибка → state = ABORTED, vote: ABORT             │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│ 3. PREPARE Database (participant 2)                         │
│    - Парсить и валидировать JSON                            │
│    - НЕ сохранять в БД! Только проверка                     │
│    - UPDATE transaction_log: state = PREPARED               │
│    - Если ошибка → state = ABORTED                          │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│ 4. COORDINATOR DECISION                                      │
│    - Если оба PREPARED → решение = COMMIT                   │
│    - Если хотя бы один ABORTED → решение = ABORT            │
└─────────────────────────────────────────────────────────────┘
```

### PHASE 2a: COMMIT (успешный путь)

```
┌─────────────────────────────────────────────────────────────┐
│ 5. COMMIT Database                                           │
│    - UPDATE transaction_log: state = COMMITTING             │
│    - Выполнить операции (INSERT/UPDATE/DELETE)              │
│    - INSERT INTO import_history                             │
│    - Транзакция БД закоммичена                              │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│ 6. COMMIT MinIO                                              │
│    - COPY staging/{txId}.json → final/{txId}.json           │
│    - DELETE staging/{txId}.json                             │
│    - UPDATE transaction_log: state = COMMITTED              │
└─────────────────────────────────────────────────────────────┘
                            ↓
                    ✅ УСПЕШНО!
```

### PHASE 2b: ABORT (откат)

```
┌─────────────────────────────────────────────────────────────┐
│ 7. ABORT (компенсация)                                      │
│    - UPDATE transaction_log: state = ABORTING               │
│    - DELETE staging/{txId}.json                             │
│    - ROLLBACK транзакции БД (@Transactional)                │
│    - UPDATE transaction_log: state = ABORTED                │
└─────────────────────────────────────────────────────────────┘
                            ↓
                    ❌ ОТКАЧЕНО
```

---

## Пример реального выполнения

### Сценарий: Успешный импорт 1 квартиры с домом

**HTTP Request:**
```http
POST /api/import HTTP/1.1
Content-Type: application/json

[
  {
    "type": "FLAT",
    "operation": "CREATE",
    "data": {
      "name": "Квартира 101",
      "floor": 1,
      "area": 50,
      "price": 5000000,
      "coordinates": {"x": 100, "y": 200},
      "house": {
        "name": "Дом на Невском",
        "year": 2020,
        "numberOfFlatsOnFloor": 4
      }
    }
  }
]
```

### Временная шкала выполнения

```
Время | Статус | Действие
------|--------|----------
t0    | -      | HTTP POST /api/import
t1    | PREPARING | TransactionCoordinator.beginTransaction()
      |        | - txId = a3f7b2c9-...
      |        | - INSERT INTO transaction_log (state=PREPARING)
t2    | PREPARING | TransactionCoordinator.prepareMinIO()
      |        | - Upload to staging/a3f7b2c9-...json
t3    | PREPARED | - MinIO vote: COMMIT ✅
      |        | - UPDATE transaction_log SET state=PREPARED
t4    | PREPARED | TransactionCoordinator.prepareDatabase()
      |        | - Parse JSON ✅
      |        | - Validate structure ✅
      |        | - Database vote: COMMIT ✅
t5    | PREPARED | Coordinator Decision: COMMIT
t6    | COMMITTING | ImportService: начало сохранения данных
      |        | - UPDATE transaction_log SET state=COMMITTING
t7    | COMMITTING | - CREATE Coordinates (id=150)
t8    | COMMITTING | - CREATE House (id=100)
t9    | COMMITTING | - Validate business rules ✅
t10   | COMMITTING | - CREATE Flat (id=201)
t11   | COMMITTING | - CREATE ImportHistory (id=123)
      |        | - UPDATE transaction_log SET import_history_id=123
t12   | COMMITTING | TransactionCoordinator.commit()
      |        | - COPY staging → final
t13   | COMMITTING | - DELETE staging file
t14   | COMMITTED ✅| - UPDATE transaction_log SET state=COMMITTED
      |        | - UPDATE import_history SET file_object_key=final/...
t15   | COMMITTED ✅| HTTP 200 OK
      |        | {"id": 123, "objectsCount": 3}
```

**Общее время**: ~800ms

---

### Сценарий: Неуспешный импорт (нарушение лимита)

**HTTP Request** (тот же, но дом уже заполнен):

### Временная шкала выполнения

```
Время | Статус | Действие
------|--------|----------
t0    | -      | HTTP POST /api/import
t1-t5 | PREPARING→PREPARED | (то же самое, что в успешном сценарии)
t6    | COMMITTING | ImportService: начало сохранения данных
t7    | COMMITTING | - CREATE Coordinates (id=151)
t8    | COMMITTING | - Используется существующий House (id=100)
t9    | COMMITTING | FlatService.validateCoordinatesAndFloorUniqueness()
      |        | - HouseRepository.findByIdWithLock(100) 🔒
      |        | - COUNT = 4 (уже есть 4 квартиры)
      |        | - 4 + 1 > 4? ДА! ❌
t10   | COMMITTING | throw UniqueConstraintViolationException
t11   | ABORTING | TransactionCoordinator.abort()
      |        | - UPDATE transaction_log SET state=ABORTING
t12   | ABORTING | - DELETE staging/a3f7b2c9-...json
t13   | ABORTING | - ROLLBACK database transaction
      |        |   (Coordinates id=151 удален, House не изменен)
t14   | ABORTED ❌ | - UPDATE transaction_log SET state=ABORTED
t15   | ABORTED ❌ | HTTP 400 Bad Request
      |        | {"message": "Нарушено ограничение уникальности..."}
```

**Общее время**: ~400ms (быстрее, т.к. не было финального коммита)

**HTTP Response:**
```json
{
  "message": "Нарушено ограничение уникальности: на координатах (1, 2) и этаже 1 уже существует 4 квартир(ы). Максимум для этого дома: 4 квартир на этаже.",
  "timestamp": "2025-12-22T12:38:50"
}
```

---

## Идемпотентность

### Проблема: что если операция повторяется?

```java
// Пример: клиент отправил запрос дважды
POST /import {same file}  // 1-й раз
POST /import {same file}  // 2-й раз (retry)
```

### Решение: проверка дубликатов

```java
// 1. Вычислить хэш файла
String fileHash = SHA256(fileContent);

// 2. Проверить, есть ли уже COMMITTED транзакция с таким хэшом
TransactionLog existing = findByFileHashAndState(fileHash, COMMITTED);
if (existing != null) {
    // Вернуть существующий ImportHistory
    return importHistoryRepository.findById(existing.getImportHistoryId());
}

// 3. Продолжить 2PC...
```

## Recovery механизм

### Зависшие транзакции (timeout)

**Scheduled job** (каждые 5 минут):

```java
@Schedule(minute = "*/5")
public void recoverTimedOutTransactions() {
    List<TransactionLog> timedOut = findTimedOutTransactions();
    
    for (TransactionLog tx : timedOut) {
        switch (tx.getState()) {
            case PREPARING:
            case PREPARED:
                // Безопасно откатить (данных в БД еще нет)
                abort(tx);
                break;
                
            case COMMITTING:
                // Попытаться завершить коммит
                retryCommit(tx);
                break;
                
            case ABORTING:
                // Завершить откат
                retryAbort(tx);
                break;
        }
    }
}
```

### Очистка staging области

**Scheduled job** (каждый день в 3:00):

```java
@Schedule(hour = "3")
public void cleanupStagingFiles() {
    // Найти все файлы в staging/ старше 24 часов
    List<String> oldFiles = minioService.listOldStagingFiles(24);
    
    for (String objectKey : oldFiles) {
        // Проверить, нет ли активной транзакции
        TransactionLog tx = findByStagingObjectKey(objectKey);
        if (tx == null || tx.getState() == ABORTED) {
            // Удалить осиротевший файл
            minioService.deleteFile(objectKey);
        }
    }
}
```

## Структура классов

```
service/
├── ImportService.java              # Основной сервис импорта (использует 2PC)
├── TransactionCoordinator.java     # Координатор 2PC
├── TransactionRecoveryService.java # Recovery зависших транзакций
└── MinioService.java              # Операции с MinIO (staging/final)

entity/
└── TransactionLog.java            # Журнал транзакций

repository/
└── TransactionLogRepository.java  # CRUD для transaction_log
```

## Сценарии отказов

### 1. Падение сервера после PREPARE MinIO

```
State: staging/{txId}.json существует, state=PREPARED
Recovery: 
  - Timeout истек -> ABORT
  - Удалить staging/{txId}.json
  - state = ABORTED
```

### 2. Падение сервера после COMMIT Database, до COMMIT MinIO

```
State: 
  - БД: ImportHistory создан, state=COMMITTING
  - MinIO: staging/{txId}.json существует, final/ пусто
Recovery:
  - Найти COMMITTING транзакции
  - Завершить: COPY staging -> final, DELETE staging
  - state = COMMITTED
```

### 3. Падение во время COPY staging -> final

```
State:
  - staging/{txId}.json существует
  - final/{txId}.json может существовать или нет
Recovery:
  - Проверить, существует ли final/{txId}.json
  - Если да -> DELETE staging, state=COMMITTED
  - Если нет -> retry COPY, затем DELETE staging
```

## Преимущества этого подхода

1. ✅ **Настоящая эмуляция 2PC**: staging area для "неподтвержденных" файлов
2. ✅ **Идемпотентность**: повторные запросы безопасны
3. ✅ **Recovery**: автоматическое восстановление зависших транзакций
4. ✅ **Консистентность**: состояние всегда определено (PREPARED/COMMITTED/ABORTED)
5. ✅ **Аудит**: полная история транзакций в transaction_log

## Недостатки / Ограничения

1. ❌ **Не атомарно**: между фазами есть временное окно несогласованности
2. ❌ **COPY в MinIO дорого**: дублирование данных при staging -> final
3. ⚠️ **Сложность**: больше кода, больше состояний

## Альтернативы COPY → DELETE

### Вариант 1: Metadata тэги (рекомендуется)

```java
// PREPARE
minioClient.putObject(bucket, objectKey, data, 
    metadata: {"status": "pending", "txId": txId});

// COMMIT
minioClient.setObjectMetadata(bucket, objectKey,
    metadata: {"status": "committed"});
```

**Преимущества**: не нужен COPY (быстрее, дешевле)

### Вариант 2: Два бакета

```
import-files-staging/   # PREPARE
import-files-final/     # COMMIT
```

**Преимущества**: четкое разделение, легко очищать staging

---

## Резюме

Архитектура 2PC обеспечивает:
- ✅ **Атомарность**: операции либо выполняются полностью, либо откатываются
- ✅ **Согласованность**: PostgreSQL и MinIO всегда синхронизированы
- ✅ **Изоляция**: пессимистичные блокировки предотвращают race conditions
- ✅ **Долговечность**: все изменения записываются в transaction_log
- ✅ **Идемпотентность**: повторные запросы безопасны благодаря хэшированию
- ✅ **Восстанавливаемость**: recovery механизм завершает зависшие транзакции

**Статусы транзакций:**
```
PREPARING → PREPARED → COMMITTING → COMMITTED ✅ (успех)
         ↘ ABORTING → ABORTED ❌ (ошибка)
```

**Полный цикл импорта:** ~800-1200ms  
**Цикл отката:** ~200-400ms


