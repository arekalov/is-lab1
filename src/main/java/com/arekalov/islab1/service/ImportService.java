package com.arekalov.islab1.service;

import com.arekalov.islab1.entity.*;
import com.arekalov.islab1.exception.UniqueConstraintViolationException;
import com.arekalov.islab1.exception.ValidationException;
import com.arekalov.islab1.repository.FlatRepository;
import com.arekalov.islab1.repository.HouseRepository;
import com.arekalov.islab1.repository.ImportHistoryRepository;
import com.arekalov.islab1.dto.request.ImportOperationRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Универсальный сервис для импорта объектов из JSON
 * Поддерживает операции CREATE, UPDATE, DELETE для Flat, House, Coordinates
 * 
 * Использует CDI с явными транзакциями для 2PC
 */
@ApplicationScoped
public class ImportService {
    
    private static final Logger logger = Logger.getLogger(ImportService.class.getName());
    
    @Inject
    private FlatRepository flatRepository;
    
    @Inject
    private HouseRepository houseRepository;
    
    @Inject
    private ImportHistoryRepository importHistoryRepository;
    
    @Inject
    private ObjectMapper objectMapper;
    
    @Inject
    private Validator validator;
    
    @Inject
    private WebSocketService webSocketService;
    
    @Inject
    private FlatService flatService;
    
    @Inject
    private TransactionCoordinator txCoordinator;
    
    /**
     * Унив

ерсальный импорт объектов
     * Принимает массив операций с разными типами объектов
     * 
     * @param json JSON строка с массивом операций
     * @return История импорта
     */
    @Transactional(Transactional.TxType.REQUIRED)
    public ImportHistory importObjects(String json) {
        return importObjectsWithFileName(json, "import-" + System.currentTimeMillis() + ".json");
    }
    
    /**
     * Универсальный импорт объектов с двухфазным коммитом (2PC)
     * 
     * Протокол с staging/final областями в MinIO:
     * PHASE 1 (PREPARE): загрузить в staging/, валидировать данные
     * PHASE 2 (COMMIT): сохранить в БД, скопировать staging -> final
     * 
     * @param json JSON строка с массивом операций
     * @param fileName имя файла для сохранения в MinIO
     * @return История импорта
     */
    @Transactional(Transactional.TxType.REQUIRED)
    public ImportHistory importObjectsWithFileName(String json, String fileName) {
        byte[] fileContent = json.getBytes();
        TransactionLog txLog = null;
        
        try {
            // BEGIN TRANSACTION
            txLog = txCoordinator.beginTransaction(fileContent, fileName);
            
            // Идемпотентность: если файл уже импортирован
            if (txLog.getState() == TransactionLog.TransactionState.COMMITTED) {
                ImportHistory existing = importHistoryRepository.findById(txLog.getImportHistoryId());
                logger.warning("File already imported, returning existing: id=" + existing.getId());
                return existing;
            }
            
            // PHASE 1: PREPARE MinIO (upload to staging)
            txLog = txCoordinator.prepareMinIO(txLog, fileContent);
            
            // PHASE 1: PREPARE Database (validate JSON format)
            txLog = txCoordinator.prepareDatabase(txLog, json);
            
            // PHASE 2: COMMIT Database (execute validated operations)
            List<ImportOperationRequest> operations = objectMapper.readValue(
                txLog.getValidatedOperations(), 
                objectMapper.getTypeFactory().constructCollectionType(List.class, ImportOperationRequest.class)
            );
            
            logger.info("");
            logger.info("╔════════════════════════════════════════════════════════╗");
            logger.info("║  PHASE 2: COMMIT Database Participant                   ║");
            logger.info("╚════════════════════════════════════════════════════════╝");
            
            int successCount = 0;
            for (int i = 0; i < operations.size(); i++) {
                ImportOperationRequest operation = operations.get(i);
                logger.info(String.format("Operation %d/%d: type=%s, operation=%s", 
                    i + 1, operations.size(), operation.getType(), operation.getOperation()));
                
                String type = operation.getType().toUpperCase();
                JsonNode dataNode = objectMapper.valueToTree(operation.getData());
                
                String op = operation.getOperation();
                if (op == null || op.isEmpty()) {
                    op = dataNode.has("id") && !dataNode.get("id").isNull() ? "UPDATE" : "CREATE";
                }
                op = op.toUpperCase();
                
                int affectedObjects = 0;
                switch (type) {
                    case "FLAT":
                        affectedObjects = processFlatOperation(op, dataNode);
                        break;
                    case "HOUSE":
                        affectedObjects = processHouseOperation(op, dataNode);
                        break;
                    case "COORDINATES":
                        affectedObjects = processCoordinatesOperation(op, dataNode);
                        break;
                    default:
                        throw new IllegalArgumentException("Неизвестный тип объекта: " + type);
                }
                successCount += affectedObjects;
            }
            
            // Создаем ImportHistory
            ImportHistory history = ImportHistory.builder()
                .operationTime(LocalDateTime.now())
                .objectsCount(successCount)
                .build();
            
            history = importHistoryRepository.save(history);
            logger.info("Database COMMIT: ImportHistory created, id=" + history.getId());
            
            // PHASE 2: COMMIT MinIO (copy staging -> final)
            txLog = txCoordinator.commit(txLog, history);
            
            // Обновляем ImportHistory с финальным путем к файлу
            history.setFileObjectKey(txLog.getFinalObjectKey());
            history = importHistoryRepository.save(history);
            
            return history;
            
        } catch (ValidationException e) {
            abortTransaction(txLog);
            logger.warning("Validation error: " + e.getMessage());
            throw e;
            
        } catch (UniqueConstraintViolationException e) {
            abortTransaction(txLog);
            logger.warning("Unique constraint violation: " + e.getMessage());
            throw e;
            
        } catch (IllegalArgumentException e) {
            abortTransaction(txLog);
            logger.warning("Business logic error: " + e.getMessage());
            throw e;
            
        } catch (InvalidFormatException e) {
            abortTransaction(txLog);
            String fieldName = e.getPath().isEmpty() ? "unknown" : 
                e.getPath().get(e.getPath().size() - 1).getFieldName();
            String value = String.valueOf(e.getValue());
            
            String message;
            if (e.getTargetType().isEnum()) {
                Object[] enumConstants = e.getTargetType().getEnumConstants();
                String validValues = Arrays.stream(enumConstants)
                    .map(Object::toString)
                    .collect(Collectors.joining(", "));
                
                message = String.format(
                    "Недопустимое значение '%s' для поля '%s'. Допустимые значения: %s",
                    value, fieldName, validValues
                );
            } else {
                message = String.format(
                    "Неверный формат значения '%s' для поля '%s'",
                    value, fieldName
                );
            }
            
            logger.warning("Format error: " + message);
            throw new IllegalArgumentException(message, e);
            
        } catch (JsonProcessingException e) {
            abortTransaction(txLog);
            logger.warning("JSON parsing error: " + e.getMessage());
            throw new IllegalArgumentException("Ошибка парсинга JSON: " + e.getOriginalMessage(), e);
            
        } catch (Exception e) {
            abortTransaction(txLog);
            logger.severe("Technical error: " + e.getClass().getName() + ": " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Техническая ошибка сервера", e);
        }
    }
    
    /**
     * Откатить транзакцию (вызывается при ошибках)
     */
    private void abortTransaction(TransactionLog txLog) {
        if (txLog != null) {
            try {
                txCoordinator.abort(txLog);
            } catch (Exception abortError) {
                logger.severe("ABORT failed: " + abortError.getMessage());
            }
        }
    }
    
    /**
     * Форматирует JSON для сохранения: удаляет поля с null значениями для id
     */
    private String formatJson(String json) {
        try {
            JsonNode rootNode = objectMapper.readTree(json);
            removeNullIds(rootNode);
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(rootNode);
        } catch (Exception e) {
            logger.warning("Ошибка форматирования JSON: " + e.getMessage());
            return json; // Возвращаем исходный JSON в случае ошибки
        }
    }
    
    /**
     * Рекурсивно удаляет поля id со значением null из JSON
     */
    private void removeNullIds(JsonNode node) {
        if (node.isObject()) {
            ObjectNode objectNode = (ObjectNode) node;
            if (objectNode.has("id") && objectNode.get("id").isNull()) {
                objectNode.remove("id");
            }
            // Рекурсивно обрабатываем вложенные объекты
            objectNode.fields().forEachRemaining(entry -> removeNullIds(entry.getValue()));
        } else if (node.isArray()) {
            // Обрабатываем массивы
            node.forEach(this::removeNullIds);
        }
    }
    
    /**
     * Обработка операции с квартирой
     * @return количество затронутых объектов (включая вложенные House/Coordinates)
     */
    private int processFlatOperation(String operation, JsonNode dataNode) throws Exception {
        switch (operation) {
            case "CREATE":
                return createFlat(dataNode);
            case "UPDATE":
                return updateFlat(dataNode);
            case "DELETE":
                return deleteFlat(dataNode);
            default:
                throw new IllegalArgumentException("Неизвестная операция: " + operation);
        }
    }
    
    /**
     * Создать квартиру
     * @return количество созданных объектов (Flat + House + Coordinates если созданы)
     */
    private int createFlat(JsonNode dataNode) throws Exception {
        int createdObjects = 0;
        
        // Обрабатываем вложенные координаты (обязательные)
        Coordinates coordinates = null;
        if (dataNode.has("coordinates")) {
            JsonNode coordsNode = dataNode.get("coordinates");
            coordinates = objectMapper.treeToValue(coordsNode, Coordinates.class);
            validateEntity(coordinates, "Координаты");
            coordinates = flatRepository.saveCoordinates(coordinates);
            createdObjects++; // +1 за Coordinates
        } else {
            throw new IllegalArgumentException("Квартира должна иметь координаты");
        }
        
        // Обрабатываем вложенный дом (опциональный)
        House house = null;
        if (dataNode.has("house") && !dataNode.get("house").isNull()) {
            JsonNode houseNode = dataNode.get("house");
            
            // Проверяем, передан ID или полный объект
            if (houseNode.isNumber()) {
                // Передан ID существующего дома
                Long houseId = houseNode.asLong();
                house = houseRepository.findById(houseId);
                if (house == null) {
                    throw new IllegalArgumentException("Дом с id=" + houseId + " не найден");
                }
                logger.info("Используется существующий дом: id=" + houseId);
                // НЕ увеличиваем счетчик - дом уже существовал
            } else {
                // Передан объект для создания нового дома
                house = objectMapper.treeToValue(houseNode, House.class);
                validateEntity(house, "Дом");
                house = houseRepository.save(house);
                createdObjects++; // +1 за House
                logger.info("Создан новый дом: id=" + house.getId());
            }
        }
        
        // Создаем копию dataNode без вложенных объектов coordinates и house
        JsonNode flatDataNode = dataNode.deepCopy();
        if (flatDataNode instanceof ObjectNode) {
            ObjectNode objectNode = (ObjectNode) flatDataNode;
            objectNode.remove("coordinates");
            objectNode.remove("house");
        }
        
        // Создаем квартиру без вложенных объектов
        Flat flat = objectMapper.treeToValue(flatDataNode, Flat.class);
        flat.setCoordinates(coordinates);
        flat.setHouse(house);
        flat.setCreationDate(LocalDateTime.now());
        flat.setId(null); // Гарантируем создание новой записи
        
        validateEntity(flat, "Квартира");
        
        // Проверка ограничений уникальности через FlatService
        flatService.validateTerribleViewConstraint(flat);
        flatService.validateCoordinatesAndFloorUniqueness(flat);
        
        flat = flatRepository.save(flat);
        createdObjects++; // +1 за Flat
        
        webSocketService.notifyFlatUpdate("CREATE", flat);
        logger.info(String.format("Создана квартира: id=%d, name=%s (всего создано объектов: %d)", 
            flat.getId(), flat.getName(), createdObjects));
        
        return createdObjects;
    }
    
    /**
     * Обновить квартиру
     * @return количество измененных объектов (всегда 1, так как обновляется только Flat)
     */
    private int updateFlat(JsonNode dataNode) throws Exception {
        Long id = dataNode.get("id").asLong();
        Flat existingFlat = flatRepository.findById(id);
        
        if (existingFlat == null) {
            throw new IllegalArgumentException("Квартира с id=" + id + " не найдена");
        }
        
        // Создаем копию dataNode без вложенных объектов coordinates и house
        JsonNode flatDataNode = dataNode.deepCopy();
        if (flatDataNode instanceof ObjectNode) {
            ObjectNode objectNode = (ObjectNode) flatDataNode;
            objectNode.remove("coordinates");
            objectNode.remove("house");
        }
        
        // Обновляем поля
        Flat updatedFlat = objectMapper.treeToValue(flatDataNode, Flat.class);
        updatedFlat.setId(id);
        updatedFlat.setCreationDate(existingFlat.getCreationDate()); // Сохраняем дату создания
        
        // Если указаны новые координаты, обновляем их
        if (dataNode.has("coordinates")) {
            Coordinates coords = objectMapper.treeToValue(dataNode.get("coordinates"), Coordinates.class);
            if (coords.getId() == null) {
                coords = flatRepository.saveCoordinates(coords);
            }
            updatedFlat.setCoordinates(coords);
        } else {
            updatedFlat.setCoordinates(existingFlat.getCoordinates());
        }
        
        // Если указан новый дом, обновляем его
        House house = null;
        if (dataNode.has("house") && !dataNode.get("house").isNull()) {
            JsonNode houseNode = dataNode.get("house");
            
            // Проверяем, передан ID или полный объект
            if (houseNode.isNumber()) {
                // Передан ID существующего дома
                Long houseId = houseNode.asLong();
                house = houseRepository.findById(houseId);
                if (house == null) {
                    throw new IllegalArgumentException("Дом с id=" + houseId + " не найден");
                }
            } else {
                // Передан объект для создания/обновления дома
                house = objectMapper.treeToValue(houseNode, House.class);
                if (house.getId() == null) {
                    validateEntity(house, "Дом");
                    house = houseRepository.save(house);
                }
            }
            updatedFlat.setHouse(house);
        } else {
            updatedFlat.setHouse(existingFlat.getHouse());
        }
        
        validateEntity(updatedFlat, "Квартира");
        
        // Проверка ограничений уникальности через FlatService
        flatService.validateTerribleViewConstraint(updatedFlat);
        flatService.validateCoordinatesAndFloorUniqueness(updatedFlat);
        
        updatedFlat = flatRepository.save(updatedFlat);
        
        webSocketService.notifyFlatUpdate("UPDATE", updatedFlat);
        logger.info("Обновлена квартира: id=" + id);
        
        return 1; // Обновлен 1 объект (Flat)
    }
    
    /**
     * Удалить квартиру
     * @return количество удаленных объектов (всегда 1)
     */
    private int deleteFlat(JsonNode dataNode) {
        Long id = dataNode.get("id").asLong();
        Flat flat = flatRepository.findById(id);
        
        if (flat == null) {
            throw new IllegalArgumentException("Квартира с id=" + id + " не найдена");
        }
        
        flatRepository.deleteById(id);
        webSocketService.notifyFlatUpdate("DELETE", flat);
        logger.info("Удалена квартира: id=" + id);
        
        return 1; // Удален 1 объект (Flat)
    }
    
    /**
     * Обработка операции с домом
     * @return количество затронутых объектов (всегда 1 для House)
     */
    private int processHouseOperation(String operation, JsonNode dataNode) throws Exception {
        switch (operation) {
            case "CREATE":
                House house = objectMapper.treeToValue(dataNode, House.class);
                house.setId(null);
                validateEntity(house, "Дом");
                house = houseRepository.save(house);
                logger.info("Создан дом: id=" + house.getId());
                return 1; // Создан 1 объект
                
            case "UPDATE":
                Long houseId = dataNode.get("id").asLong();
                House existingHouse = houseRepository.findById(houseId);
                if (existingHouse == null) {
                    throw new IllegalArgumentException("Дом с id=" + houseId + " не найден");
                }
                house = objectMapper.treeToValue(dataNode, House.class);
                house.setId(houseId);
                validateEntity(house, "Дом");
                house = houseRepository.save(house);
                logger.info("Обновлен дом: id=" + houseId);
                return 1; // Обновлен 1 объект
                
            case "DELETE":
                houseId = dataNode.get("id").asLong();
                existingHouse = houseRepository.findById(houseId);
                if (existingHouse == null) {
                    throw new IllegalArgumentException("Дом с id=" + houseId + " не найден");
                }
                houseRepository.deleteById(houseId);
                logger.info("Удален дом: id=" + houseId);
                return 1; // Удален 1 объект
                
            default:
                throw new IllegalArgumentException("Неизвестная операция: " + operation);
        }
    }
    
    /**
     * Обработка операции с координатами
     * @return количество затронутых объектов (всегда 1 для Coordinates)
     */
    private int processCoordinatesOperation(String operation, JsonNode dataNode) throws Exception {
        switch (operation) {
            case "CREATE":
                Coordinates coords = objectMapper.treeToValue(dataNode, Coordinates.class);
                coords.setId(null);
                validateEntity(coords, "Координаты");
                coords = flatRepository.saveCoordinates(coords);
                logger.info("Созданы координаты: id=" + coords.getId());
                return 1; // Создан 1 объект
                
            case "UPDATE":
                Long coordsId = dataNode.get("id").asLong();
                coords = objectMapper.treeToValue(dataNode, Coordinates.class);
                coords.setId(coordsId);
                validateEntity(coords, "Координаты");
                coords = flatRepository.saveCoordinates(coords);
                logger.info("Обновлены координаты: id=" + coordsId);
                return 1; // Обновлен 1 объект
                
            case "DELETE":
                throw new UnsupportedOperationException("Удаление координат не поддерживается (используются квартирами)");
                
            default:
                throw new IllegalArgumentException("Неизвестная операция: " + operation);
        }
    }
    
    /**
     * Валидация сущности через Bean Validation
     */
    private void validateEntity(Object entity, String entityName) {
        Set<ConstraintViolation<Object>> violations = validator.validate(entity);
        
        if (!violations.isEmpty()) {
            StringBuilder errorMessage = new StringBuilder(entityName + " содержит ошибки валидации:\n");
            for (ConstraintViolation<Object> violation : violations) {
                errorMessage.append("- ").append(violation.getPropertyPath())
                    .append(": ").append(violation.getMessage()).append("\n");
            }
            throw new ValidationException(errorMessage.toString());
        }
    }
}
