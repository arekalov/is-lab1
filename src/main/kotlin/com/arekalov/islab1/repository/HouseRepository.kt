package com.arekalov.islab1.repository

import com.arekalov.islab1.entity.House
import jakarta.enterprise.context.ApplicationScoped
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Репозиторий для работы с домами
 */
@ApplicationScoped
class HouseRepository {
    
    @PersistenceContext(unitName = "flatsPU")
    private lateinit var entityManager: EntityManager
    
    /**
     * Найти все дома
     */
    suspend fun findAll(): List<House> = withContext(Dispatchers.IO) {
        entityManager.createQuery("SELECT h FROM House h ORDER BY h.id", House::class.java)
            .resultList
    }
    
    /**
     * Найти дом по ID
     */
    suspend fun findById(id: Long): House? = withContext(Dispatchers.IO) {
        entityManager.find(House::class.java, id)
    }
    
    /**
     * Сохранить дом
     */
    suspend fun save(house: House): House = withContext(Dispatchers.IO) {
        if (house.id == null) {
            entityManager.persist(house)
            house
        } else {
            entityManager.merge(house)
        }
    }
    
    /**
     * Удалить дом по ID
     */
    suspend fun deleteById(id: Long): Boolean = withContext(Dispatchers.IO) {
        val house = findById(id)
        if (house != null) {
            entityManager.remove(house)
            true
        } else {
            false
        }
    }
    
    /**
     * Найти дома по названию
     */
    suspend fun findByNameContaining(substring: String): List<House> = withContext(Dispatchers.IO) {
        entityManager.createQuery(
            "SELECT h FROM House h WHERE LOWER(h.name) LIKE LOWER(:substring)",
            House::class.java
        )
            .setParameter("substring", "%$substring%")
            .resultList
    }
}
