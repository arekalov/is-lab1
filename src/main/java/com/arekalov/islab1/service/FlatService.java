package com.arekalov.islab1.service;

import com.arekalov.islab1.entity.Flat;
import com.arekalov.islab1.entity.House;
import com.arekalov.islab1.entity.View;
import com.arekalov.islab1.exception.UniqueConstraintViolationException;
import com.arekalov.islab1.repository.FlatRepository;
import com.arekalov.islab1.repository.HouseRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.logging.Logger;

/**
 * Сервис для работы с квартирами с поддержкой JPA транзакций
 */
@ApplicationScoped
public class FlatService {
    
    private static final Logger logger = Logger.getLogger(FlatService.class.getName());
    
    @Inject
    private FlatRepository flatRepository;
    
    @Inject
    private HouseRepository houseRepository;
    
    @Inject
    private WebSocketService webSocketService;
    
    /**
     * Получить все квартиры с пагинацией
     */
    public List<Flat> getAllFlats(int page, int size, String sortBy) {
        logger.info("FlatService.getAllFlats() - получение квартир: page=" + page + ", size=" + size + ", sortBy=" + sortBy);
        
        try {
            List<Flat> flats = flatRepository.findAll(page, size, sortBy);
            logger.info("FlatService.getAllFlats() - получено квартир: " + flats.size());
            return flats;
            
        } catch (Exception e) {
            logger.severe("FlatService.getAllFlats() - ошибка получения квартир: " + e.getMessage());
            throw new RuntimeException("Ошибка получения квартир: " + e.getMessage(), e);
        }
    }
    
    /**
     * Получить общее количество квартир
     */
    public long countFlats() {
        logger.info("FlatService.countFlats() - подсчет общего количества квартир");
        
        try {
            Long count = flatRepository.count();
            logger.info("FlatService.countFlats() - общее количество квартир: " + count);
            return count;
            
        } catch (Exception e) {
            logger.severe("FlatService.countFlats() - ошибка подсчета квартир: " + e.getMessage());
            throw new RuntimeException("Ошибка подсчета квартир: " + e.getMessage(), e);
        }
    }
    
    /**
     * Получить все квартиры без пагинации
     */
    public List<Flat> getAllFlats() {
        return getAllFlats(0, 20, "id");
    }
    
    /**
     * Найти квартиру по ID
     */
    public Flat getFlatById(Long id) {
        logger.info("FlatNativeService.getFlatById() - поиск квартиры с id=" + id);
        
        try {
            Flat flat = flatRepository.findById(id);
            
            if (flat != null) {
                logger.info("FlatNativeService.getFlatById() - квартира найдена: " + flat.getName());
            } else {
                logger.info("FlatNativeService.getFlatById() - квартира не найдена");
            }
            
            return flat;
            
        } catch (Exception e) {
            logger.severe("FlatNativeService.getFlatById() - ошибка поиска: " + e.getMessage());
            throw new RuntimeException("Ошибка поиска квартиры: " + e.getMessage(), e);
        }
    }
    
    /**
     * Создать новую квартиру
     * Транзакция REQUIRED - создает новую транзакцию или использует существующую
     */
    @Transactional(Transactional.TxType.REQUIRED)
    public Flat createFlat(Flat flat) {
        logger.info("FlatNativeService.createFlat() - создание квартиры: " + flat.getName());
        
        try {
            // Валидация
            validateFlat(flat);
            
            // Устанавливаем дату создания
            flat.setCreationDate(LocalDateTime.now());
            
            // Проверяем и создаем координаты, если нужно
            if (flat.getCoordinates() != null && flat.getCoordinates().getId() == null) {
                logger.info("FlatNativeService.createFlat() - создание новых координат");
            }
            
            // Проверяем существование дома, если указан
            if (flat.getHouse() != null && flat.getHouse().getId() != null) {
                House house = houseRepository.findById(flat.getHouse().getId());
                if (house == null) {
                    throw new RuntimeException("Дом с ID " + flat.getHouse().getId() + " не найден");
                }
                flat.setHouse(house);
                logger.info("FlatNativeService.createFlat() - привязка к дому: " + house.getName());
            }
            
            // Проверка ограничений уникальности
            validateTerribleViewConstraint(flat);
            validateCoordinatesAndFloorUniqueness(flat);
            
            // Сохраняем квартиру
            Flat savedFlat = flatRepository.save(flat);
            
            logger.info("FlatNativeService.createFlat() - квартира создана с id=" + savedFlat.getId());
            
            // Отправляем уведомление через WebSocket
            webSocketService.notifyFlatUpdate("CREATE", savedFlat);
            
            return savedFlat;
            
        } catch (Exception e) {
            logger.severe("FlatNativeService.createFlat() - ошибка создания: " + e.getMessage());
            throw new RuntimeException("Ошибка создания квартиры: " + e.getMessage(), e);
        }
    }
    
    /**
     * Обновить квартиру
     * Транзакция REQUIRED - создает новую транзакцию или использует существующую
     */
    @Transactional(Transactional.TxType.REQUIRED)
    public Flat updateFlat(Long id, Flat updatedFlat) {
        logger.info("FlatNativeService.updateFlat() - обновление квартиры с id=" + id);
        
        try {
            // Проверяем существование квартиры
            Flat existingFlat = flatRepository.findById(id);
            if (existingFlat == null) {
                throw new RuntimeException("Квартира с ID " + id + " не найдена");
            }
            
            // ВАЖНО: Устанавливаем значения по умолчанию из существующей квартиры
            // для полей, которые могут быть не переданы при обновлении
            if (updatedFlat.getFloor() == null) {
                updatedFlat.setFloor(existingFlat.getFloor());
            }
            
            // Валидация с учетом заполненных полей
            validateFlat(updatedFlat);
            
            // Обновляем поля
            existingFlat.setName(updatedFlat.getName());
            existingFlat.setArea(updatedFlat.getArea());
            existingFlat.setPrice(updatedFlat.getPrice());
            existingFlat.setBalcony(updatedFlat.getBalcony());
            existingFlat.setTimeToMetroOnFoot(updatedFlat.getTimeToMetroOnFoot());
            existingFlat.setNumberOfRooms(updatedFlat.getNumberOfRooms());
            existingFlat.setLivingSpace(updatedFlat.getLivingSpace());
            existingFlat.setFurnish(updatedFlat.getFurnish());
            existingFlat.setView(updatedFlat.getView());
            existingFlat.setFloor(updatedFlat.getFloor()); // Теперь floor точно не null
            
            // Обновляем координаты
            if (updatedFlat.getCoordinates() != null) {
                if (existingFlat.getCoordinates() != null) {
                    existingFlat.getCoordinates().setX(updatedFlat.getCoordinates().getX());
                    existingFlat.getCoordinates().setY(updatedFlat.getCoordinates().getY());
                } else {
                    existingFlat.setCoordinates(updatedFlat.getCoordinates());
                }
            }
            
            // Обновляем дом, если указан
            if (updatedFlat.getHouse() != null && updatedFlat.getHouse().getId() != null) {
                House house = houseRepository.findById(updatedFlat.getHouse().getId());
                if (house == null) {
                    throw new RuntimeException("Дом с ID " + updatedFlat.getHouse().getId() + " не найден");
                }
                existingFlat.setHouse(house);
                logger.info("FlatNativeService.updateFlat() - обновлена привязка к дому: " + house.getName());
            } else if (updatedFlat.getHouse() == null) {
                // Явно передан null - удаляем привязку к дому
                existingFlat.setHouse(null);
            }
            // Если updatedFlat.getHouse().getId() == null, оставляем существующий дом
            
            // Проверка ограничений уникальности
            // Важно: проверяем только если есть дом, иначе проверки не применимы
            if (existingFlat.getHouse() != null) {
                validateTerribleViewConstraint(existingFlat);
                validateCoordinatesAndFloorUniqueness(existingFlat);
            }
            
            // Сохраняем изменения
            Flat savedFlat = flatRepository.save(existingFlat);
            
            logger.info("FlatNativeService.updateFlat() - квартира обновлена");
            
            // Отправляем уведомление через WebSocket
            webSocketService.notifyFlatUpdate("UPDATE", savedFlat);
            
            return savedFlat;
            
        } catch (Exception e) {
            logger.severe("FlatNativeService.updateFlat() - ошибка обновления: " + e.getMessage());
            throw new RuntimeException("Ошибка обновления квартиры: " + e.getMessage(), e);
        }
    }
    
    /**
     * Удалить квартиру
     * Транзакция REQUIRED - создает новую транзакцию или использует существующую
     */
    @Transactional(Transactional.TxType.REQUIRED)
    public boolean deleteFlat(Long id) {
        logger.info("FlatNativeService.deleteFlat() - удаление квартиры с id=" + id);
        
        try {
            boolean deleted = flatRepository.deleteById(id);
            
            if (deleted) {
                logger.info("FlatNativeService.deleteFlat() - квартира успешно удалена");
                // Отправляем уведомление через WebSocket
                webSocketService.notifyFlatUpdate("DELETE", id);
            } else {
                logger.info("FlatNativeService.deleteFlat() - квартира не найдена для удаления");
            }
            
            return deleted;
            
        } catch (Exception e) {
            logger.severe("FlatNativeService.deleteFlat() - ошибка удаления: " + e.getMessage());
            throw new RuntimeException("Ошибка удаления квартиры: " + e.getMessage(), e);
        }
    }
    
    /**
     * Валидация квартиры
     */
    private void validateFlat(Flat flat) {
        if (flat.getName() == null || flat.getName().trim().isEmpty()) {
            throw new RuntimeException("Название квартиры не может быть пустым");
        }
        
        if (flat.getArea() == null || flat.getArea() <= 0) {
            throw new RuntimeException("Площадь квартиры должна быть больше 0");
        }
        
        if (flat.getPrice() == null || flat.getPrice() <= 0) {
            throw new RuntimeException("Цена квартиры должна быть больше 0");
        }
        
        if (flat.getTimeToMetroOnFoot() != null && flat.getTimeToMetroOnFoot() <= 0) {
            throw new RuntimeException("Время до метро должно быть больше 0");
        }
        
        if (flat.getNumberOfRooms() == null || flat.getNumberOfRooms() <= 0 || flat.getNumberOfRooms() > 16) {
            throw new RuntimeException("Количество комнат должно быть от 1 до 16");
        }
        
        if (flat.getLivingSpace() != null && flat.getLivingSpace() <= 0) {
            throw new RuntimeException("Жилая площадь должна быть больше 0");
        }
        
        if (flat.getFloor() == null || flat.getFloor() <= 0) {
            throw new RuntimeException("Этаж должен быть больше 0");
        }
        
        if (flat.getCoordinates() == null) {
            throw new RuntimeException("Координаты квартиры обязательны");
        }
        
        // Валидация координат
        if (flat.getCoordinates().getX() == null) {
            throw new RuntimeException("Координата X обязательна");
        }
        
        if (flat.getCoordinates().getY() == null || flat.getCoordinates().getY() <= -515) {
            throw new RuntimeException("Координата Y должна быть больше -515");
        }
        
        logger.info("FlatNativeService.validateFlat() - валидация прошла успешно");
    }
    
    /**
     * Проверка ограничения: не больше половины квартир на этаже могут иметь вид BAD (некрасивый)
     * ВАЖНО: Использует пессимистическую блокировку дома для предотвращения race conditions
     */
    public void validateTerribleViewConstraint(Flat flat) {
        // Если у квартиры нет дома или вид не BAD, проверка не нужна
        if (flat.getHouse() == null || flat.getView() != View.BAD) {
            return;
        }
        
        Long houseId = flat.getHouse().getId();
        Integer floor = flat.getFloor();
        
        if (houseId == null || floor == null) {
            return;
        }
        
        // ⚠️ КЛЮЧЕВОЙ МОМЕНТ: Блокируем дом для предотвращения race conditions
        // Пока блокировка удерживается, другие потоки не смогут проверять/добавлять квартиры
        House house = houseRepository.findByIdWithLock(houseId);
        
        if (house == null || house.getNumberOfFlatsOnFloor() == null) {
            return;
        }
        
        Integer numberOfFlatsOnFloor = house.getNumberOfFlatsOnFloor();
        
        logger.info(String.format(
            "Проверка ограничения BAD (некрасивый вид) для квартиры: house_id=%d, floor=%d, flats_per_floor=%d [LOCKED]",
            houseId, floor, numberOfFlatsOnFloor
        ));
        
        // Используем репозиторий для подсчета
        Long badCount = flatRepository.countByHouseAndFloorAndView(houseId, floor, View.BAD, flat.getId());
        
        // Если добавляем/обновляем квартиру с BAD, увеличиваем счетчик
        long newBadCount = badCount + 1;
        
        // Максимально допустимое количество квартир с BAD (половина от всех квартир на этаже)
        double maxBadAllowed = numberOfFlatsOnFloor / 2.0;
        
        logger.info(String.format(
            "Текущее количество BAD: %d, после добавления: %d, максимум: %.1f",
            badCount, newBadCount, maxBadAllowed
        ));
        
        if (newBadCount > maxBadAllowed) {
            String message = String.format(
                "Нарушено ограничение уникальности: на этаже %d дома (ID=%d) не может быть больше %.0f квартир с некрасивым видом (BAD). " +
                "Сейчас: %d, попытка добавить еще одну.",
                floor, houseId, maxBadAllowed, badCount
            );
            throw new UniqueConstraintViolationException(message);
        }
        
        logger.info("Проверка ограничения BAD пройдена успешно [LOCK WILL BE RELEASED]");
    }
    
    /**
     * Проверка уникальности: не может быть больше numberOfFlatsOnFloor квартир 
     * с одинаковыми координатами и этажом
     * ВАЖНО: Использует пессимистическую блокировку дома для предотвращения race conditions
     */
    public void validateCoordinatesAndFloorUniqueness(Flat flat) {
        if (flat.getCoordinates() == null || flat.getFloor() == null) {
            return;
        }
        
        // Для проверки нужен дом с numberOfFlatsOnFloor
        if (flat.getHouse() == null) {
            return;
        }
        
        Long houseId = flat.getHouse().getId();
        Integer floor = flat.getFloor();
        
        if (houseId == null) {
            return;
        }
        
        // ⚠️ КЛЮЧЕВОЙ МОМЕНТ: Блокируем дом для предотвращения race conditions
        House house = houseRepository.findByIdWithLock(houseId);
        
        if (house == null || house.getNumberOfFlatsOnFloor() == null) {
            return;
        }
        
        Integer x = flat.getCoordinates().getX();
        Integer y = flat.getCoordinates().getY();
        Integer numberOfFlatsOnFloor = house.getNumberOfFlatsOnFloor();
        
        if (x == null || y == null) {
            return;
        }
        
        logger.info(String.format(
            "Проверка уникальности координат+этаж для квартиры: x=%d, y=%d, floor=%d, max=%d [LOCKED]",
            x, y, floor, numberOfFlatsOnFloor
        ));
        
        // Используем репозиторий для подсчета
        Long currentCount = flatRepository.countByCoordinatesAndFloor(x, y, floor, flat.getId());
        long newCount = currentCount + 1; // После добавления/обновления
        
        logger.info(String.format(
            "Текущее количество квартир с координатами (%d, %d) на этаже %d: %d, после добавления: %d, максимум: %d",
            x, y, floor, currentCount, newCount, numberOfFlatsOnFloor
        ));
        
        if (newCount > numberOfFlatsOnFloor) {
            String message = String.format(
                "Нарушено ограничение уникальности: на координатах (%d, %d) и этаже %d уже существует %d квартир(ы). " +
                "Максимум для этого дома: %d квартир на этаже.",
                x, y, floor, currentCount, numberOfFlatsOnFloor
            );
            throw new UniqueConstraintViolationException(message);
        }
        
        logger.info("Проверка уникальности координат+этаж пройдена успешно [LOCK WILL BE RELEASED]");
    }
    
    /**
     * Подсчитать количество квартир с количеством комнат больше заданного
     */
    public Long countByRoomsGreaterThan(Integer minRooms) {
        logger.info("FlatService.countByRoomsGreaterThan() - подсчет квартир с комнатами > " + minRooms);
        
        try {
            Long count = flatRepository.countByRoomsGreaterThan(minRooms);
            logger.info("FlatService.countByRoomsGreaterThan() - найдено квартир: " + count);
            return count;
            
        } catch (Exception e) {
            logger.severe("FlatService.countByRoomsGreaterThan() - ошибка: " + e.getMessage());
            throw new RuntimeException("Ошибка подсчета квартир по комнатам: " + e.getMessage(), e);
        }
    }
    
    /**
     * Найти квартиры, содержащие подстроку в названии
     */
    public List<Flat> findByNameContaining(String nameSubstring) {
        logger.info("FlatService.findByNameContaining() - поиск квартир с названием содержащим: " + nameSubstring);
        
        try {
            List<Flat> flats = flatRepository.findByNameContaining(nameSubstring);
            logger.info("FlatService.findByNameContaining() - найдено квартир: " + flats.size());
            return flats;
            
        } catch (Exception e) {
            logger.severe("FlatService.findByNameContaining() - ошибка: " + e.getMessage());
            throw new RuntimeException("Ошибка поиска квартир по названию: " + e.getMessage(), e);
        }
    }
    
    /**
     * Найти квартиры с жилой площадью меньше заданной
     */
    public List<Flat> findByLivingSpaceLessThan(Long maxSpace) {
        logger.info("FlatService.findByLivingSpaceLessThan() - поиск квартир с площадью < " + maxSpace);
        
        try {
            List<Flat> flats = flatRepository.findByLivingSpaceLessThan(maxSpace);
            logger.info("FlatService.findByLivingSpaceLessThan() - найдено квартир: " + flats.size());
            return flats;
            
        } catch (Exception e) {
            logger.severe("FlatService.findByLivingSpaceLessThan() - ошибка: " + e.getMessage());
            throw new RuntimeException("Ошибка поиска квартир по жилой площади: " + e.getMessage(), e);
        }
    }
    
    /**
     * Найти самую дешевую квартиру с балконом
     */
    public Flat findCheapestWithBalcony() {
        logger.info("FlatService.findCheapestWithBalcony() - поиск самой дешевой квартиры с балконом");
        
        try {
            Flat flat = flatRepository.findCheapestWithBalcony();
            
            if (flat != null) {
                logger.info("FlatService.findCheapestWithBalcony() - найдена квартира: " + flat.getName() + ", цена: " + flat.getPrice());
            } else {
                logger.info("FlatService.findCheapestWithBalcony() - квартиры с балконом не найдены");
            }
            
            return flat;
            
        } catch (Exception e) {
            logger.severe("FlatService.findCheapestWithBalcony() - ошибка: " + e.getMessage());
            throw new RuntimeException("Ошибка поиска самой дешевой квартиры с балконом: " + e.getMessage(), e);
        }
    }
    
    /**
     * Найти все квартиры, отсортированные по времени до метро
     */
    public List<Flat> findAllSortedByMetroTime() {
        logger.info("FlatService.findAllSortedByMetroTime() - поиск всех квартир, отсортированных по времени до метро");
        
        try {
            List<Flat> flats = flatRepository.findAllSortedByMetroTime();
            logger.info("FlatService.findAllSortedByMetroTime() - найдено квартир: " + flats.size());
            return flats;
            
        } catch (Exception e) {
            logger.severe("FlatService.findAllSortedByMetroTime() - ошибка: " + e.getMessage());
            throw new RuntimeException("Ошибка поиска квартир, отсортированных по времени до метро: " + e.getMessage(), e);
        }
    }
}
