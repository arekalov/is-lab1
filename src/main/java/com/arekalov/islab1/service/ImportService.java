package com.arekalov.islab1.service;

import com.arekalov.islab1.entity.*;
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
import jakarta.ejb.Stateless;
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
 */
@Stateless
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
    
    /**
     * Универсальный импорт объектов
     * Принимает массив операций с разными типами объектов
     * 
     * @param json JSON строка с массивом операций
     * @return История импорта
     */
    @Transactional(Transactional.TxType.REQUIRED)
    public ImportHistory importObjects(String json) {
        logger.info("ImportService.importObjects() - начало импорта");
        
        try {
            // Парсим JSON в массив операций
            List<ImportOperationRequest> operations = objectMapper.readValue(
                json, 
                objectMapper.getTypeFactory().constructCollectionType(List.class, ImportOperationRequest.class)
            );
            
            if (operations == null || operations.isEmpty()) {
                throw new IllegalArgumentException("Массив операций пуст");
            }
            
            logger.info("ImportService.importObjects() - найдено " + operations.size() + " операций");
            
            // Счетчик успешно выполненных операций
            int successCount = 0;
            
            // Обрабатываем каждую операцию
            for (int i = 0; i < operations.size(); i++) {
                ImportOperationRequest operation = operations.get(i);
                logger.info(String.format("ImportService.importObjects() - операция %d: type=%s, operation=%s", 
                    i + 1, operation.getType(), operation.getOperation()));
                
                // Определяем тип объекта и операцию
                String type = operation.getType().toUpperCase();
                JsonNode dataNode = objectMapper.valueToTree(operation.getData());
                
                // Автоопределение операции по наличию id
                String op = operation.getOperation();
                if (op == null || op.isEmpty()) {
                    op = dataNode.has("id") && !dataNode.get("id").isNull() ? "UPDATE" : "CREATE";
                }
                op = op.toUpperCase();
                
                logger.info("ImportService.importObjects() - выполняется: " + type + " " + op);
                
                // Выполняем операцию в зависимости от типа
                switch (type) {
                    case "FLAT":
                        processFlatOperation(op, dataNode);
                        break;
                    case "HOUSE":
                        processHouseOperation(op, dataNode);
                        break;
                    case "COORDINATES":
                        processCoordinatesOperation(op, dataNode);
                        break;
                    default:
                        throw new IllegalArgumentException("Неизвестный тип объекта: " + type);
                }
                successCount++;
            }
            
            // Форматируем JSON для сохранения (удаляем поля с null id и форматируем)
            String formattedJson = formatJson(json);
            
            // Создаем запись в истории импорта с сохранением исходного JSON
            ImportHistory history = ImportHistory.builder()
                .operationTime(LocalDateTime.now())
                .objectsCount(successCount)
                .changesDescription(formattedJson)
                .build();
            
            history = importHistoryRepository.save(history);
            logger.info("ImportService.importObjects() - импорт успешно завершен, история id=" + history.getId());
            
            return history;
            
        } catch (ValidationException e) {
            // Ошибки валидации - пробрасываем как есть
            logger.warning("ImportService.importObjects() - ошибка валидации: " + e.getMessage());
            throw e;
            
        } catch (IllegalArgumentException e) {
            // Ошибки бизнес-логики - пробрасываем как есть
            logger.warning("ImportService.importObjects() - ошибка бизнес-логики: " + e.getMessage());
            throw e;
            
        } catch (InvalidFormatException e) {
            // Jackson ошибки форматирования (например, некорректный enum)
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
            
            logger.warning("ImportService.importObjects() - ошибка формата данных: " + message);
            throw new IllegalArgumentException(message, e);
            
        } catch (JsonProcessingException e) {
            // Другие Jackson ошибки парсинга
            logger.warning("ImportService.importObjects() - ошибка парсинга JSON: " + e.getMessage());
            throw new IllegalArgumentException("Ошибка парсинга JSON: " + e.getOriginalMessage(), e);
            
        } catch (Exception e) {
            // Настоящие технические ошибки
            logger.severe("ImportService.importObjects() - техническая ошибка: " + e.getClass().getName() + ": " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Техническая ошибка сервера", e);
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
     */
    private void processFlatOperation(String operation, JsonNode dataNode) throws Exception {
        switch (operation) {
            case "CREATE":
                createFlat(dataNode);
                break;
            case "UPDATE":
                updateFlat(dataNode);
                break;
            case "DELETE":
                deleteFlat(dataNode);
                break;
            default:
                throw new IllegalArgumentException("Неизвестная операция: " + operation);
        }
    }
    
    /**
     * Создать квартиру
     */
    private void createFlat(JsonNode dataNode) throws Exception {
        // Обрабатываем вложенные координаты (обязательные)
        Coordinates coordinates = null;
        if (dataNode.has("coordinates")) {
            JsonNode coordsNode = dataNode.get("coordinates");
            coordinates = objectMapper.treeToValue(coordsNode, Coordinates.class);
            validateEntity(coordinates, "Координаты");
            coordinates = flatRepository.saveCoordinates(coordinates);
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
            } else {
                // Передан объект для создания нового дома
                house = objectMapper.treeToValue(houseNode, House.class);
                validateEntity(house, "Дом");
                house = houseRepository.save(house);
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
        flat = flatRepository.save(flat);
        
        webSocketService.notifyFlatUpdate("CREATE", flat);
        logger.info("Создана квартира: id=" + flat.getId() + ", name=" + flat.getName());
    }
    
    /**
     * Обновить квартиру
     */
    private void updateFlat(JsonNode dataNode) throws Exception {
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
        updatedFlat = flatRepository.save(updatedFlat);
        
        webSocketService.notifyFlatUpdate("UPDATE", updatedFlat);
        logger.info("Обновлена квартира: id=" + id);
    }
    
    /**
     * Удалить квартиру
     */
    private void deleteFlat(JsonNode dataNode) {
        Long id = dataNode.get("id").asLong();
        Flat flat = flatRepository.findById(id);
        
        if (flat == null) {
            throw new IllegalArgumentException("Квартира с id=" + id + " не найдена");
        }
        
        flatRepository.deleteById(id);
        webSocketService.notifyFlatUpdate("DELETE", flat);
        logger.info("Удалена квартира: id=" + id);
    }
    
    /**
     * Обработка операции с домом
     */
    private void processHouseOperation(String operation, JsonNode dataNode) throws Exception {
        switch (operation) {
            case "CREATE":
                House house = objectMapper.treeToValue(dataNode, House.class);
                house.setId(null);
                validateEntity(house, "Дом");
                house = houseRepository.save(house);
                logger.info("Создан дом: id=" + house.getId());
                break;
                
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
                break;
                
            case "DELETE":
                houseId = dataNode.get("id").asLong();
                existingHouse = houseRepository.findById(houseId);
                if (existingHouse == null) {
                    throw new IllegalArgumentException("Дом с id=" + houseId + " не найден");
                }
                houseRepository.deleteById(houseId);
                logger.info("Удален дом: id=" + houseId);
                break;
                
            default:
                throw new IllegalArgumentException("Неизвестная операция: " + operation);
        }
    }
    
    /**
     * Обработка операции с координатами
     */
    private void processCoordinatesOperation(String operation, JsonNode dataNode) throws Exception {
        switch (operation) {
            case "CREATE":
                Coordinates coords = objectMapper.treeToValue(dataNode, Coordinates.class);
                coords.setId(null);
                validateEntity(coords, "Координаты");
                coords = flatRepository.saveCoordinates(coords);
                logger.info("Созданы координаты: id=" + coords.getId());
                break;
                
            case "UPDATE":
                Long coordsId = dataNode.get("id").asLong();
                coords = objectMapper.treeToValue(dataNode, Coordinates.class);
                coords.setId(coordsId);
                validateEntity(coords, "Координаты");
                coords = flatRepository.saveCoordinates(coords);
                logger.info("Обновлены координаты: id=" + coordsId);
                break;
                
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
