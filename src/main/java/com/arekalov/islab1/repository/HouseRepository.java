package com.arekalov.islab1.repository;

import com.arekalov.islab1.entity.House;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import java.util.List;

/**
 * Репозиторий для работы с домами
 */
@ApplicationScoped
public class HouseRepository {
    
    @PersistenceContext(unitName = "flatsPU")
    private EntityManager entityManager;
    
    /**
     * Найти все дома
     */
    public List<House> findAll() {
        TypedQuery<House> query = entityManager.createQuery(
            "SELECT h FROM House h ORDER BY h.id", House.class);
        return query.getResultList();
    }
    
    /**
     * Найти дом по ID
     */
    public House findById(Long id) {
        return entityManager.find(House.class, id);
    }
    
    /**
     * Сохранить дом
     */
    public House save(House house) {
        if (house.getId() == null) {
            entityManager.persist(house);
            return house;
        } else {
            return entityManager.merge(house);
        }
    }
    
    /**
     * Удалить дом по ID
     */
    public boolean deleteById(Long id) {
        House house = findById(id);
        if (house != null) {
            entityManager.remove(house);
            return true;
        }
        return false;
    }
    
    /**
     * Найти дома по названию
     */
    public List<House> findByNameContaining(String substring) {
        TypedQuery<House> query = entityManager.createQuery(
            "SELECT h FROM House h WHERE LOWER(h.name) LIKE LOWER(:substring)",
            House.class
        );
        query.setParameter("substring", "%" + substring + "%");
        return query.getResultList();
    }
}
