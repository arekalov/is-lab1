package com.arekalov.islab1.repository;

import com.arekalov.islab1.entity.Flat;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Репозиторий для работы с квартирами
 */
@ApplicationScoped
public class FlatRepository {
    
    @PersistenceContext(unitName = "flatsPU")
    private EntityManager entityManager;
    
    /**
     * Найти все квартиры с пагинацией
     */
    public List<Flat> findAll(int page, int size, String sortBy) {
        return entityManager.createNativeQuery(
            "SELECT * FROM flats ORDER BY " + sortBy + " LIMIT ? OFFSET ?", Flat.class)
            .setParameter(1, size)
            .setParameter(2, page * size)
            .getResultList();
    }
    
    public List<Flat> findAll() {
        return findAll(0, 20, "id");
    }
    
    /**
     * Подсчитать общее количество квартир
     */
    public Long count() {
        return entityManager.createQuery("SELECT COUNT(f) FROM Flat f", Long.class)
            .getSingleResult();
    }
    
    /**
     * Найти квартиру по ID
     */
    public Flat findById(Long id) {
        return entityManager.find(Flat.class, id);
    }
    
    /**
     * Сохранить квартиру
     */
    public Flat save(Flat flat) {
        if (flat.getId() == null) {
            entityManager.persist(flat);
            return flat;
        } else {
            return entityManager.merge(flat);
        }
    }
    
    /**
     * Удалить квартиру по ID
     */
    public boolean deleteById(Long id) {
        Flat flat = findById(id);
        if (flat != null) {
            entityManager.remove(flat);
            return true;
        }
        return false;
    }
    
    /**
     * Найти квартиры по названию (содержит подстроку)
     */
    public List<Flat> findByNameContaining(String substring) {
        TypedQuery<Flat> query = entityManager.createQuery(
            "SELECT f FROM Flat f WHERE LOWER(f.name) LIKE LOWER(:substring)",
            Flat.class
        );
        query.setParameter("substring", "%" + substring + "%");
        return query.getResultList();
    }
    
    /**
     * Подсчитать квартиры с количеством комнат больше заданного
     */
    public Long countByNumberOfRoomsGreaterThan(Integer minRooms) {
        TypedQuery<Long> query = entityManager.createQuery(
            "SELECT COUNT(f) FROM Flat f WHERE f.numberOfRooms > :minRooms",
            Long.class
        );
        query.setParameter("minRooms", minRooms);
        return query.getSingleResult();
    }
    
    /**
     * Найти квартиры с жилой площадью меньше заданной
     */
    public List<Flat> findByLivingSpaceLessThan(Long maxSpace) {
        TypedQuery<Flat> query = entityManager.createQuery(
            "SELECT f FROM Flat f WHERE f.livingSpace < :maxSpace",
            Flat.class
        );
        query.setParameter("maxSpace", maxSpace);
        return query.getResultList();
    }
    
    /**
     * Найти самую дешевую квартиру с балконом
     */
    public Flat findCheapestWithBalcony() {
        TypedQuery<Flat> query = entityManager.createQuery(
            "SELECT f FROM Flat f WHERE f.balcony = true ORDER BY f.price ASC",
            Flat.class
        );
        query.setMaxResults(1);
        List<Flat> results = query.getResultList();
        return results.isEmpty() ? null : results.get(0);
    }
    
    /**
     * Найти все квартиры, отсортированные по времени до метро
     */
    public List<Flat> findAllSortedByMetroTime() {
        TypedQuery<Flat> query = entityManager.createQuery(
            "SELECT f FROM Flat f ORDER BY f.timeToMetroOnFoot ASC",
            Flat.class
        );
        return query.getResultList();
    }
    
    /**
     * Поиск с фильтрацией по различным полям
     */
    public List<Flat> findWithFilters(String nameFilter, Long minPrice, Long maxPrice,
                                     Boolean hasBalcony, Integer minRooms, Integer maxRooms,
                                     int page, int size, String sortBy, String sortDirection) {
        
        StringBuilder queryBuilder = new StringBuilder("SELECT f FROM Flat f WHERE 1=1");
        Map<String, Object> parameters = new HashMap<>();
        
        if (nameFilter != null && !nameFilter.trim().isEmpty()) {
            queryBuilder.append(" AND LOWER(f.name) LIKE LOWER(:nameFilter)");
            parameters.put("nameFilter", "%" + nameFilter + "%");
        }
        
        if (minPrice != null) {
            queryBuilder.append(" AND f.price >= :minPrice");
            parameters.put("minPrice", minPrice);
        }
        
        if (maxPrice != null) {
            queryBuilder.append(" AND f.price <= :maxPrice");
            parameters.put("maxPrice", maxPrice);
        }
        
        if (hasBalcony != null) {
            queryBuilder.append(" AND f.balcony = :hasBalcony");
            parameters.put("hasBalcony", hasBalcony);
        }
        
        if (minRooms != null) {
            queryBuilder.append(" AND f.numberOfRooms >= :minRooms");
            parameters.put("minRooms", minRooms);
        }
        
        if (maxRooms != null) {
            queryBuilder.append(" AND f.numberOfRooms <= :maxRooms");
            parameters.put("maxRooms", maxRooms);
        }
        
        queryBuilder.append(" ORDER BY f.").append(sortBy).append(" ").append(sortDirection);
        
        TypedQuery<Flat> query = entityManager.createQuery(queryBuilder.toString(), Flat.class);
        
        for (Map.Entry<String, Object> entry : parameters.entrySet()) {
            query.setParameter(entry.getKey(), entry.getValue());
        }
        
        query.setFirstResult(page * size);
        query.setMaxResults(size);
        
        return query.getResultList();
    }
    
    // Перегруженный метод с дефолтными значениями
    public List<Flat> findWithFilters(String nameFilter, Long minPrice, Long maxPrice,
                                     Boolean hasBalcony, Integer minRooms, Integer maxRooms) {
        return findWithFilters(nameFilter, minPrice, maxPrice, hasBalcony, minRooms, maxRooms,
                              0, 20, "id", "ASC");
    }
}
