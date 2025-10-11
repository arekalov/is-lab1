package com.arekalov.islab1.repository;

import com.arekalov.islab1.entity.House;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceUnit;
import jakarta.persistence.TypedQuery;
import java.util.List;

/**
 * Репозиторий для работы с домами (без транзакций)
 */
@ApplicationScoped
public class HouseRepository {
    
    // @PersistenceUnit(unitName = "flatsPU") // Временно отключено для нативного EclipseLink
    private EntityManagerFactory entityManagerFactory;
    
    /**
     * Найти все дома
     */
    public List<House> findAll() {
        EntityManager em = entityManagerFactory.createEntityManager();
        try {
            TypedQuery<House> query = em.createQuery(
                "SELECT h FROM House h ORDER BY h.id", House.class);
            return query.getResultList();
        } finally {
            em.close();
        }
    }
    
    /**
     * Найти дом по ID
     */
    public House findById(Long id) {
        EntityManager em = entityManagerFactory.createEntityManager();
        try {
            return em.find(House.class, id);
        } finally {
            em.close();
        }
    }
    
    /**
     * Сохранить дом (без транзакций - опасно!)
     */
    public House save(House house) {
        EntityManager em = entityManagerFactory.createEntityManager();
        try {
            // В режиме без транзакций используем auto-commit
            if (house.getId() == null) {
                em.persist(house);
                em.flush(); 
                return house;
            } else {
                House merged = em.merge(house);
                em.flush(); 
                return merged;
            }
        } finally {
            em.close();
        }
    }
    
    /**
     * Удалить дом по ID (без транзакций - опасно!)
     */
    public boolean deleteById(Long id) {
        EntityManager em = entityManagerFactory.createEntityManager();
        try {
            House house = em.find(House.class, id);
            if (house != null) {
                em.remove(house);
                em.flush(); // Принудительно отправляем в БД
                return true;
            }
            return false;
        } finally {
            em.close();
        }
    }
    
    /**
     * Найти дома по названию
     */
    public List<House> findByNameContaining(String substring) {
        EntityManager em = entityManagerFactory.createEntityManager();
        try {
            TypedQuery<House> query = em.createQuery(
                "SELECT h FROM House h WHERE LOWER(h.name) LIKE LOWER(:substring)",
                House.class
            );
            query.setParameter("substring", "%" + substring + "%");
            return query.getResultList();
        } finally {
            em.close();
        }
    }
}
