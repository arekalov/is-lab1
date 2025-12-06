# План доработки ИС для Лабораторной работы 2
# Дата: 2025-12-06
# Статус: В процессе

## ✅ Этап 0: Подготовка (ГОТОВО)
- [x] Рефакторинг на JPA с EclipseLink
- [x] Добавление поля floor в Flat
- [x] Создание Mapper'ов
- [x] Разделение DTO на Request/Response
- [x] Переименование pojo → entity

---

## 🔄 Этап 1: Транзакции и изоляция

### 1.1. Добавить аннотацию @Transactional в сервисы
**Файлы:** `FlatService.java`, `HouseService.java`

- [ ] Добавить `@Transactional` на методы создания
- [ ] Добавить `@Transactional` на методы обновления
- [ ] Добавить `@Transactional` на методы удаления
- [ ] Методы чтения оставить без транзакций (или READ_ONLY)

```java
@Transactional(Transactional.TxType.REQUIRED)
public Flat createFlat(Flat flat) { ... }

@Transactional(Transactional.TxType.REQUIRED)
public Flat updateFlat(Flat flat) { ... }

@Transactional(Transactional.TxType.REQUIRED)
public boolean deleteFlat(Long id) { ... }
```

### 1.2. Настроить уровни изоляции (для Этапа 5 - JMeter)
- [ ] Изучить конфликты при конкурентном доступе
- [ ] Настроить SERIALIZABLE для критичных операций
- [ ] Документировать обоснование выбора

---

## 📊 Этап 2: Таблица истории импорта

### 2.1. Создать Entity ImportHistory
**Файл:** `src/main/java/com/arekalov/islab1/entity/ImportHistory.java`

```java
@Entity
@Table(name = "import_history")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImportHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "operation_time", nullable = false)
    private LocalDateTime operationTime;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ImportStatus status; // SUCCESS, FAILED
    
    @Enumerated(EnumType.STRING)
    @Column(name = "entity_type", nullable = false)
    private EntityType entityType; // FLAT, HOUSE, COORDINATES
    
    @Column(name = "objects_count")
    private Integer objectsCount; // для SUCCESS
    
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage; // для FAILED
    
    @Column(name = "changes_description", columnDefinition = "TEXT")
    private String changesDescription; // JSON с деталями
}
```

### 2.2. Создать Enums
- [ ] `ImportStatus` enum (SUCCESS, FAILED)
- [ ] `EntityType` enum (FLAT, HOUSE, COORDINATES)

### 2.3. Создать Response DTO
**Файл:** `src/main/java/com/arekalov/islab1/dto/response/ImportHistoryResponseDTO.java`

### 2.4. Создать Repository
**Файл:** `src/main/java/com/arekalov/islab1/repository/ImportHistoryRepository.java`

```java
@ApplicationScoped
public class ImportHistoryRepository {
    @Inject
    private EntityManagerService entityManagerService;
    
    public ImportHistory save(ImportHistory history) { ... }
    public List<ImportHistory> findAll(int page, int size) { ... }
    public long count() { ... }
}
```

### 2.5. Создать Mapper
**Файл:** `src/main/java/com/arekalov/islab1/mapper/ImportHistoryMapper.java`

---

## 🔒 Этап 3: Бизнес-ограничения уникальности

### 3.1. Создать сервис проверки ограничений
**Файл:** `src/main/java/com/arekalov/islab1/service/UniqueConstraintsService.java`

```java
@ApplicationScoped
public class UniqueConstraintsService {
    @Inject
    private EntityManagerService entityManagerService;
    
    /**
     * Проверка: не более половины квартир на этаже могут иметь TERRIBLE вид
     * @throws UniqueConstraintViolationException если проверка не пройдена
     */
    public void validateTerribleViewConstraint(Flat flat) {
        if (flat.getView() != View.TERRIBLE) {
            return; // проверка только для TERRIBLE
        }
        
        if (flat.getHouse() == null) {
            return; // нет дома - нет ограничения
        }
        
        // Подсчитать квартиры с TERRIBLE на этаже
        Long terribleCount = countTerribleViewOnFloor(
            flat.getHouse().getId(), 
            flat.getFloor()
        );
        
        // Получить максимальное допустимое количество
        Integer maxFlatsOnFloor = flat.getHouse().getNumberOfFlatsOnFloor();
        long maxTerribleAllowed = maxFlatsOnFloor / 2;
        
        if (terribleCount >= maxTerribleAllowed) {
            throw new UniqueConstraintViolationException(
                String.format("На этаже %d дома %s уже %d квартир с ужасным видом. " +
                             "Максимум разрешено: %d (половина от %d)",
                    flat.getFloor(), 
                    flat.getHouse().getName(),
                    terribleCount,
                    maxTerribleAllowed,
                    maxFlatsOnFloor)
            );
        }
    }
    
    private Long countTerribleViewOnFloor(Long houseId, Integer floor) {
        EntityManager em = entityManagerService.getEntityManager();
        TypedQuery<Long> query = em.createQuery(
            "SELECT COUNT(f) FROM Flat f " +
            "WHERE f.house.id = :houseId " +
            "AND f.floor = :floor " +
            "AND f.view = :view",
            Long.class
        );
        query.setParameter("houseId", houseId);
        query.setParameter("floor", floor);
        query.setParameter("view", View.TERRIBLE);
        return query.getSingleResult();
    }
}
```

### 3.2. Создать exception
**Файл:** `src/main/java/com/arekalov/islab1/exception/UniqueConstraintViolationException.java`

```java
public class UniqueConstraintViolationException extends RuntimeException {
    public UniqueConstraintViolationException(String message) {
        super(message);
    }
}
```

### 3.3. Создать ExceptionMapper
**Файл:** `src/main/java/com/arekalov/islab1/exception/UniqueConstraintViolationExceptionMapper.java`

```java
@Provider
public class UniqueConstraintViolationExceptionMapper 
    implements ExceptionMapper<UniqueConstraintViolationException> {
    
    @Override
    public Response toResponse(UniqueConstraintViolationException e) {
        return Response.status(Response.Status.CONFLICT)
            .entity(new ErrorResponse(e.getMessage()))
            .build();
    }
}
```

### 3.4. Интегрировать в FlatService
- [ ] Добавить вызов `uniqueConstraintsService.validateTerribleViewConstraint(flat)` в `createFlat()`
- [ ] Добавить вызов в `updateFlat()`

---

## 📥 Этап 4: Массовый импорт

### 4.1. Создать Request DTO для импорта
**Файл:** `src/main/java/com/arekalov/islab1/dto/request/ImportFlatRequest.java`

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ImportFlatRequest {
    @Valid
    private List<CreateFlatRequest> flats;
}
```

Аналогично для `ImportHouseRequest`, `ImportCoordinatesRequest`

### 4.2. Создать ImportService
**Файл:** `src/main/java/com/arekalov/islab1/service/ImportService.java`

```java
@ApplicationScoped
public class ImportService {
    @Inject
    private FlatMapper flatMapper;
    @Inject
    private FlatRepository flatRepository;
    @Inject
    private UniqueConstraintsService uniqueConstraintsService;
    @Inject
    private ImportHistoryRepository importHistoryRepository;
    @Inject
    private WebSocketService webSocketService;
    
    /**
     * Импорт квартир в одной транзакции
     * Если хотя бы одна квартира не прошла валидацию - откат всей транзакции
     */
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public ImportHistoryResponseDTO importFlats(List<CreateFlatRequest> requests) {
        ImportHistory history = ImportHistory.builder()
            .operationTime(LocalDateTime.now())
            .entityType(EntityType.FLAT)
            .build();
        
        try {
            List<Flat> flats = new ArrayList<>();
            StringBuilder changesDesc = new StringBuilder();
            
            // Валидация и конвертация всех объектов
            for (CreateFlatRequest request : requests) {
                Flat flat = flatMapper.fromCreateRequest(request);
                
                // Проверка бизнес-ограничений
                uniqueConstraintsService.validateTerribleViewConstraint(flat);
                
                // Сохранение (каскадно создает координаты и дом если нужно)
                Flat saved = flatRepository.save(flat);
                flats.add(saved);
                
                changesDesc.append(saved.getId()).append(",");
            }
            
            // Успешное завершение
            history.setStatus(ImportStatus.SUCCESS);
            history.setObjectsCount(flats.size());
            history.setChangesDescription(changesDesc.toString());
            
            // Уведомление через WebSocket
            webSocketService.notifyImportCompleted("FLAT", flats.size());
            
        } catch (Exception e) {
            // Откат транзакции произойдет автоматически
            history.setStatus(ImportStatus.FAILED);
            history.setErrorMessage(e.getMessage());
            throw e; // перебрасываем для отката
        } finally {
            // Сохраняем историю (в отдельной транзакции)
            importHistoryRepository.save(history);
        }
        
        return importHistoryMapper.toResponseDTO(history);
    }
}
```

### 4.3. Создать ImportController
**Файл:** `src/main/java/com/arekalov/islab1/controller/ImportController.java`

```java
@Path("/import")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ImportController {
    @Inject
    private ImportService importService;
    
    @POST
    @Path("/flats")
    public Response importFlats(@Valid List<CreateFlatRequest> requests) {
        try {
            ImportHistoryResponseDTO result = importService.importFlats(requests);
            return Response.status(Response.Status.CREATED).entity(result).build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ErrorResponse("Ошибка импорта: " + e.getMessage()))
                .build();
        }
    }
    
    @GET
    @Path("/history")
    public Response getHistory(
        @QueryParam("page") @DefaultValue("0") int page,
        @QueryParam("size") @DefaultValue("10") int size
    ) {
        // Получение истории импорта с пагинацией
    }
}
```

### 4.4. Обновить WebSocketService
- [ ] Добавить метод `notifyImportCompleted(String entityType, int count)`

---

## 🧪 Этап 5: Apache JMeter сценарии

### 5.1. Создать сценарии
- [ ] Сценарий 1: Конкурентное создание квартир
- [ ] Сценарий 2: Конкурентное обновление одной квартиры
- [ ] Сценарий 3: Конкурентное удаление
- [ ] Сценарий 4: Импорт от нескольких пользователей
- [ ] Сценарий 5: Проверка ограничения TERRIBLE view при конкуренции

### 5.2. Тестирование изоляции транзакций
- [ ] READ_COMMITTED - проверить dirty reads
- [ ] REPEATABLE_READ - проверить non-repeatable reads
- [ ] SERIALIZABLE - проверить phantom reads
- [ ] Документировать результаты и обоснование выбора

### 5.3. Оптимизация
- [ ] Настроить connection pool
- [ ] Добавить индексы для производительности
- [ ] Оптимизировать запросы

---

## 📝 Этап 6: Документация

- [ ] README.md с описанием ограничений уникальности
- [ ] API документация для endpoints импорта
- [ ] Отчет по JMeter тестированию
- [ ] Обоснование уровней изоляции транзакций

---

## ✨ Дополнительные улучшения (опционально)

- [ ] Валидация вложенных объектов при импорте
- [ ] Batch операции для производительности
- [ ] Метрики и логирование импорта
- [ ] Rate limiting для защиты от перегрузки

---

## 🎯 Порядок выполнения (рекомендуемый):

1. **Этап 1** → Транзакции (быстро, важно)
2. **Этап 3** → Ограничения уникальности (ключевой функционал)
3. **Этап 2** → История импорта (инфраструктура)
4. **Этап 4** → Импорт (основной функционал)
5. **Этап 5** → JMeter (тестирование и оптимизация)
6. **Этап 6** → Документация

---

## 📊 Прогресс:

- [x] Этап 0: Подготовка - **100%**
- [x] Этап 1: Транзакции - **100%** ✅
- [ ] Этап 2: История импорта - **0%**
- [ ] Этап 3: Ограничения - **0%**
- [ ] Этап 4: Импорт - **0%**
- [ ] Этап 5: JMeter - **0%**
- [ ] Этап 6: Документация - **0%**

**Общий прогресс: ~28%** (подготовка + транзакции)

