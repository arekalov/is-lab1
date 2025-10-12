package com.arekalov.islab1.repository;

import com.arekalov.islab1.pojo.Flat;
import com.arekalov.islab1.pojo.Coordinates;
import com.arekalov.islab1.pojo.House;
import com.arekalov.islab1.pojo.Furnish;
import com.arekalov.islab1.pojo.View;
import com.arekalov.islab1.service.DatabaseSessionService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.persistence.sessions.DatabaseSession;
import org.eclipse.persistence.queries.ReadAllQuery;
import org.eclipse.persistence.queries.ReadObjectQuery;
import org.eclipse.persistence.queries.DataReadQuery;
import org.eclipse.persistence.expressions.ExpressionBuilder;
import org.eclipse.persistence.expressions.Expression;
import org.eclipse.persistence.sessions.DatabaseRecord;

import java.util.List;
import java.util.ArrayList;
import java.util.logging.Logger;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Репозиторий для работы с квартирами на нативном EclipseLink API
 * Использует централизованный DatabaseSessionService для управления сессией
 */
@ApplicationScoped
public class FlatRepository {
    
    private static final Logger logger = Logger.getLogger(FlatRepository.class.getName());
    
    @Inject
    private DatabaseSessionService sessionService;
    
    /**
     * Получить активную DatabaseSession
     */
    private DatabaseSession getSession() {
        return sessionService.getDatabaseSession();
    }
    
    /**
     * Найти все квартиры с пагинацией через EclipseLink API
     */
    public List<Flat> findAll(int page, int size, String sortBy) {
        logger.info("FlatRepository.findAll() - поиск квартир: page=" + page + ", size=" + size + ", sortBy=" + sortBy);
        
        // Валидация параметров
        if (page < 0) {
            logger.warning("FlatRepository.findAll() - page не может быть отрицательным: " + page);
            page = 0;
        }
        if (size <= 0) {
            logger.warning("FlatRepository.findAll() - size должен быть положительным: " + size);
            size = 10; // default size
        }
        if (size > 100) {
            logger.warning("FlatRepository.findAll() - size слишком большой: " + size);
            size = 100; // max size
        }
        
        try {
            DatabaseSession session = getSession();
            
            // Рассчитываем offset
            int offset = page * size;
            logger.info("FlatRepository.findAll() - рассчитанные параметры: offset=" + offset + ", limit=" + size);
            
            // Определяем поле сортировки
            String orderByField = "id"; // default
            if ("name".equals(sortBy)) {
                orderByField = "name";
            } else if ("price".equals(sortBy)) {
                orderByField = "price";
            } else if ("area".equals(sortBy)) {
                orderByField = "area";
            }
            
            // Создаем прямой SQL запрос с LIMIT и OFFSET
            String sql = "SELECT id, area, balcony, creation_date, furnish, living_space, name, " +
                        "number_of_rooms, price, time_to_metro_on_foot, view, coordinates_id, house_id " +
                        "FROM flats ORDER BY " + orderByField + " ASC LIMIT " + size + " OFFSET " + offset;
            
            logger.info("FlatRepository.findAll() - выполняем SQL: " + sql);
            
            // Создаем DataReadQuery для выполнения прямого SQL
            DataReadQuery dataQuery = new DataReadQuery();
            dataQuery.setSQLString(sql);
            
            // Выполняем запрос и получаем результат через безопасный метод
            @SuppressWarnings("unchecked")
            List<DatabaseRecord> records = (List<DatabaseRecord>) sessionService.executeQuery(dataQuery);
            
            // Конвертируем DatabaseRecord в объекты Flat
            List<Flat> flats = new ArrayList<>();
            for (DatabaseRecord record : records) {
                Flat flat = convertRecordToFlat(record, session);
                if (flat != null) {
                    flats.add(flat);
                }
            }
            
            logger.info("FlatRepository.findAll() - найдено квартир: " + flats.size());
            return flats;
            
        } catch (Exception e) {
            logger.severe("Ошибка поиска квартир: " + e.getMessage());
            e.printStackTrace(); // Добавляем stack trace для отладки
            throw new RuntimeException("Error finding flats: " + e.getMessage(), e);
        }
    }
    
    public List<Flat> findAll() {
        return findAll(0, 20, "id");
    }
    
    /**
     * Подсчитать общее количество квартир
     */
    public Long count() {
        logger.info("FlatRepository.count() - подсчет общего количества квартир");
        
        try {
            DatabaseSession session = getSession();
            
            ReadAllQuery query = new ReadAllQuery(Flat.class);
            
            @SuppressWarnings("unchecked")
            List<Flat> flats = (List<Flat>) session.executeQuery(query);
            
            long count = flats.size();
            logger.info("FlatRepository.count() - общее количество квартир: " + count);
            return count;
            
        } catch (Exception e) {
            logger.severe("Ошибка подсчета квартир: " + e.getMessage());
            throw new RuntimeException("Error counting flats: " + e.getMessage(), e);
        }
    }
    
    /**
     * Найти квартиру по ID
     */
    public Flat findById(Long id) {
        logger.info("FlatNativeRepository.findById() - поиск квартиры с id=" + id);
        
        try {
            DatabaseSession session = getSession();
            
            ReadObjectQuery query = new ReadObjectQuery(Flat.class);
            ExpressionBuilder builder = query.getExpressionBuilder();
            Expression criteria = builder.get("id").equal(id);
            query.setSelectionCriteria(criteria);
            
            Flat flat = (Flat) session.executeQuery(query);
            
            if (flat != null) {
                logger.info("FlatNativeRepository.findById() - квартира найдена: " + flat.getName());
            } else {
                logger.info("FlatNativeRepository.findById() - квартира не найдена");
            }
            
            return flat;
            
        } catch (Exception e) {
            logger.severe("Ошибка поиска квартиры по ID: " + e.getMessage());
            throw new RuntimeException("Error finding flat by id: " + e.getMessage(), e);
        }
    }
    
    /**
     * Сохранить квартиру
     */
    public Flat save(Flat flat) {
        logger.info("FlatNativeRepository.save() - сохранение квартиры: " + flat.getName());
        
        try {
            DatabaseSession session = getSession();
            
            // Сначала сохраняем координаты, если они новые
            if (flat.getCoordinates() != null && flat.getCoordinates().getId() == null) {
                session.insertObject(flat.getCoordinates());
            }
            
            // Сохраняем квартиру
            if (flat.getId() == null) {
                session.insertObject(flat);
                logger.info("FlatNativeRepository.save() - квартира создана с id=" + flat.getId());
            } else {
                session.updateObject(flat);
                logger.info("FlatNativeRepository.save() - квартира обновлена с id=" + flat.getId());
            }
            
            return flat;
            
        } catch (Exception e) {
            logger.severe("Ошибка сохранения квартиры: " + e.getMessage());
            throw new RuntimeException("Error saving flat: " + e.getMessage(), e);
        }
    }
    
    /**
     * Удалить квартиру по ID
     */
    public boolean deleteById(Long id) {
        logger.info("FlatNativeRepository.deleteById() - удаление квартиры с id=" + id);
        
        try {
            DatabaseSession session = getSession();
            
            // Сначала найдем квартиру
            Flat flat = findById(id);
            if (flat == null) {
                logger.info("FlatNativeRepository.deleteById() - квартира не найдена для удаления");
                return false;
            }
            
            // Удаляем квартиру
            session.deleteObject(flat);
            
            // Удаляем связанные координаты, если они есть и не используются другими квартирами
            if (flat.getCoordinates() != null) {
                Long coordinatesId = flat.getCoordinates().getId();
                if (coordinatesId != null && !isCoordinatesUsedByOtherFlats(coordinatesId)) {
                    session.deleteObject(flat.getCoordinates());
                    logger.info("FlatNativeRepository.deleteById() - удалены неиспользуемые координаты с id=" + coordinatesId);
                }
            }
            
            logger.info("FlatNativeRepository.deleteById() - квартира успешно удалена");
            return true;
            
        } catch (Exception e) {
            logger.severe("Ошибка удаления квартиры: " + e.getMessage());
            throw new RuntimeException("Error deleting flat: " + e.getMessage(), e);
        }
    }
    
    /**
     * Проверить, используются ли координаты другими квартирами
     */
    private boolean isCoordinatesUsedByOtherFlats(Long coordinatesId) {
        try {
            DatabaseSession session = getSession();
            
            ReadAllQuery query = new ReadAllQuery(Flat.class);
            ExpressionBuilder builder = query.getExpressionBuilder();
            Expression criteria = builder.get("coordinates").get("id").equal(coordinatesId);
            query.setSelectionCriteria(criteria);
            
            @SuppressWarnings("unchecked")
            List<Flat> flats = (List<Flat>) session.executeQuery(query);
            
            return flats.size() > 1; // Больше 1 означает, что есть другие квартиры
            
        } catch (Exception e) {
            logger.severe("Ошибка проверки использования координат: " + e.getMessage());
            return true; // В случае ошибки лучше не удалять координаты
        }
    }
    
    /**
     * Подсчитать количество квартир с количеством комнат больше заданного
     */
    public Long countByRoomsGreaterThan(Integer minRooms) {
        logger.info("FlatRepository.countByRoomsGreaterThan() - подсчет квартир с комнатами > " + minRooms);
        
        try {
            DatabaseSession session = getSession();
            
            ReadAllQuery query = new ReadAllQuery(Flat.class);
            ExpressionBuilder builder = query.getExpressionBuilder();
            Expression criteria = builder.get("numberOfRooms").greaterThan(minRooms);
            query.setSelectionCriteria(criteria);
            
            @SuppressWarnings("unchecked")
            List<Flat> flats = (List<Flat>) session.executeQuery(query);
            
            long count = flats.size();
            logger.info("FlatRepository.countByRoomsGreaterThan() - найдено квартир: " + count);
            return count;
            
        } catch (Exception e) {
            logger.severe("Ошибка подсчета квартир по комнатам: " + e.getMessage());
            throw new RuntimeException("Error counting flats by rooms: " + e.getMessage(), e);
        }
    }
    
    /**
     * Найти квартиры, содержащие подстроку в названии
     */
    public List<Flat> findByNameContaining(String nameSubstring) {
        logger.info("FlatRepository.findByNameContaining() - поиск квартир с названием содержащим: " + nameSubstring);
        
        try {
            DatabaseSession session = getSession();
            
            // Приводим поисковую строку к нижнему регистру и убираем лишние пробелы
            String searchString = nameSubstring.trim().toLowerCase();
            
            ReadAllQuery query = new ReadAllQuery(Flat.class);
            ExpressionBuilder builder = query.getExpressionBuilder();
            
            // Поиск без учета регистра
            Expression criteria = builder.get("name").toLowerCase().like("%" + searchString + "%");
            query.setSelectionCriteria(criteria);
            
            // Сортировка результатов по имени
            query.addOrdering(builder.get("name").ascending());
            
            @SuppressWarnings("unchecked")
            List<Flat> flats = (List<Flat>) session.executeQuery(query);
            
            logger.info("FlatRepository.findByNameContaining() - найдено квартир: " + flats.size());
            return flats;
            
        } catch (Exception e) {
            logger.severe("Ошибка поиска квартир по названию: " + e.getMessage());
            throw new RuntimeException("Error finding flats by name: " + e.getMessage(), e);
        }
    }
    
    /**
     * Найти квартиры с жилой площадью меньше заданной
     */
    public List<Flat> findByLivingSpaceLessThan(Long maxSpace) {
        logger.info("FlatRepository.findByLivingSpaceLessThan() - поиск квартир с площадью < " + maxSpace);
        
        try {
            DatabaseSession session = getSession();
            
            ReadAllQuery query = new ReadAllQuery(Flat.class);
            ExpressionBuilder builder = query.getExpressionBuilder();
            Expression criteria = builder.get("livingSpace").lessThan(maxSpace);
            query.setSelectionCriteria(criteria);
            
            @SuppressWarnings("unchecked")
            List<Flat> flats = (List<Flat>) session.executeQuery(query);
            
            logger.info("FlatRepository.findByLivingSpaceLessThan() - найдено квартир: " + flats.size());
            return flats;
            
        } catch (Exception e) {
            logger.severe("Ошибка поиска квартир по жилой площади: " + e.getMessage());
            throw new RuntimeException("Error finding flats by living space: " + e.getMessage(), e);
        }
    }
    
    /**
     * Найти самую дешевую квартиру с балконом
     */
    public Flat findCheapestWithBalcony() {
        logger.info("FlatRepository.findCheapestWithBalcony() - поиск самой дешевой квартиры с балконом");
        
        try {
            DatabaseSession session = getSession();
            
            ReadAllQuery query = new ReadAllQuery(Flat.class);
            ExpressionBuilder builder = query.getExpressionBuilder();
            Expression criteria = builder.get("balcony").equal(true);
            query.setSelectionCriteria(criteria);
            query.addOrdering(builder.get("price").ascending());
            query.setMaxRows(1);
            
            @SuppressWarnings("unchecked")
            List<Flat> flats = (List<Flat>) session.executeQuery(query);
            
            if (!flats.isEmpty()) {
                Flat cheapest = flats.get(0);
                logger.info("FlatRepository.findCheapestWithBalcony() - найдена квартира: " + cheapest.getName() + ", цена: " + cheapest.getPrice());
                return cheapest;
            } else {
                logger.info("FlatRepository.findCheapestWithBalcony() - квартиры с балконом не найдены");
                return null;
            }
            
        } catch (Exception e) {
            logger.severe("Ошибка поиска самой дешевой квартиры с балконом: " + e.getMessage());
            throw new RuntimeException("Error finding cheapest flat with balcony: " + e.getMessage(), e);
        }
    }
    
    /**
     * Найти все квартиры, отсортированные по времени до метро
     */
    public List<Flat> findAllSortedByMetroTime() {
        logger.info("FlatRepository.findAllSortedByMetroTime() - поиск всех квартир, отсортированных по времени до метро");
        
        try {
            DatabaseSession session = getSession();
            
            ReadAllQuery query = new ReadAllQuery(Flat.class);
            ExpressionBuilder builder = query.getExpressionBuilder();
            query.addOrdering(builder.get("timeToMetroOnFoot").ascending());
            
            @SuppressWarnings("unchecked")
            List<Flat> flats = (List<Flat>) session.executeQuery(query);
            
            logger.info("FlatRepository.findAllSortedByMetroTime() - найдено квартир: " + flats.size());
            return flats;
            
        } catch (Exception e) {
            logger.severe("Ошибка поиска квартир, отсортированных по времени до метро: " + e.getMessage());
            throw new RuntimeException("Error finding flats sorted by metro time: " + e.getMessage(), e);
        }
    }
    
    /**
     * Конвертирует DatabaseRecord из базы данных в объект Flat
     */
    private Flat convertRecordToFlat(DatabaseRecord record, DatabaseSession session) {
        try {
            Flat flat = new Flat();
            
            // Основные поля
            Object idObj = record.get("id");
            if (idObj != null) {
                flat.setId(((Number) idObj).longValue());
            }

            flat.setName((String) record.get("name"));

            Object areaObj = record.get("area");
            if (areaObj != null) {
                flat.setArea(((Number) areaObj).longValue());
            }

            Object priceObj = record.get("price");
            if (priceObj != null) {
                flat.setPrice(((Number) priceObj).longValue());
            }

            Object livingSpaceObj = record.get("living_space");
            if (livingSpaceObj != null) {
                flat.setLivingSpace(((Number) livingSpaceObj).longValue());
            }

            Object roomsObj = record.get("number_of_rooms");
            if (roomsObj != null) {
                flat.setNumberOfRooms(((Number) roomsObj).intValue());
            }

            Object metroTimeObj = record.get("time_to_metro_on_foot");
            if (metroTimeObj != null) {
                flat.setTimeToMetroOnFoot(((Number) metroTimeObj).longValue());
            }
            
            // Булевое поле
            Object balconyValue = record.get("balcony");
            if (balconyValue != null) {
                flat.setBalcony((Boolean) balconyValue);
            }
            
            // Enum поля
            String furnishStr = (String) record.get("furnish");
            if (furnishStr != null) {
                flat.setFurnish(Furnish.valueOf(furnishStr));
            }
            
            String viewStr = (String) record.get("view");
            if (viewStr != null) {
                flat.setView(View.valueOf(viewStr));
            }
            
            // ZonedDateTime поле
            Object creationDateValue = record.get("creation_date");
            if (creationDateValue != null) {
                if (creationDateValue instanceof java.sql.Timestamp) {
                    java.sql.Timestamp timestamp = (java.sql.Timestamp) creationDateValue;
                    flat.setCreationDate(timestamp.toLocalDateTime().atZone(java.time.ZoneId.systemDefault()));
                } else if (creationDateValue instanceof String) {
                    flat.setCreationDate(ZonedDateTime.parse((String) creationDateValue, DateTimeFormatter.ISO_ZONED_DATE_TIME));
                }
            }
            
            // Связанные объекты - загружаем отдельно
            Object coordinatesIdObj = record.get("coordinates_id");
            if (coordinatesIdObj != null) {
                Long coordinatesId = ((Number) coordinatesIdObj).longValue();
                Coordinates coordinates = findCoordinatesById(coordinatesId, session);
                if (coordinates != null) {
                    flat.setCoordinates(coordinates);
                }
            }
            
            Object houseIdObj = record.get("house_id");
            if (houseIdObj != null) {
                Long houseId = ((Number) houseIdObj).longValue();
                House house = findHouseById(houseId, session);
                if (house != null) {
                    flat.setHouse(house);
                }
            }
            
            return flat;
            
        } catch (Exception e) {
            logger.severe("Ошибка конвертации Record в Flat: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Находит координаты по ID
     */
    private Coordinates findCoordinatesById(Long id, DatabaseSession session) {
        try {
            ReadObjectQuery query = new ReadObjectQuery(Coordinates.class);
            Expression expression = query.getExpressionBuilder().get("id").equal(id);
            query.setSelectionCriteria(expression);
            return (Coordinates) session.executeQuery(query);
        } catch (Exception e) {
            logger.warning("Не удалось загрузить координаты с ID " + id + ": " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Находит дом по ID
     */
    private House findHouseById(Long id, DatabaseSession session) {
        try {
            ReadObjectQuery query = new ReadObjectQuery(House.class);
            Expression expression = query.getExpressionBuilder().get("id").equal(id);
            query.setSelectionCriteria(expression);
            return (House) session.executeQuery(query);
        } catch (Exception e) {
            logger.warning("Не удалось загрузить дом с ID " + id + ": " + e.getMessage());
            return null;
        }
    }
}
