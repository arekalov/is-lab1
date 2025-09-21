package com.arekalov.islab1.repository

import com.arekalov.islab1.entity.Flat
import jakarta.enterprise.context.ApplicationScoped
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import jakarta.persistence.TypedQuery
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Репозиторий для работы с квартирами
 */
@ApplicationScoped
class FlatRepository {
    
    @PersistenceContext(unitName = "flatsPU")
    private lateinit var entityManager: EntityManager
    
    /**
     * Найти все квартиры с пагинацией
     */
    suspend fun findAll(page: Int = 0, size: Int = 20, sortBy: String = "id"): List<Flat> = withContext(Dispatchers.IO) {
        val query: TypedQuery<Flat> = entityManager.createQuery(
            "SELECT f FROM Flat f ORDER BY f.$sortBy", 
            Flat::class.java
        )
        query.firstResult = page * size
        query.maxResults = size
        query.resultList
    }
    
    /**
     * Подсчитать общее количество квартир
     */
    suspend fun count(): Long = withContext(Dispatchers.IO) {
        entityManager.createQuery("SELECT COUNT(f) FROM Flat f", Long::class.java)
            .singleResult
    }
    
    /**
     * Найти квартиру по ID
     */
    suspend fun findById(id: Long): Flat? = withContext(Dispatchers.IO) {
        entityManager.find(Flat::class.java, id)
    }
    
    /**
     * Сохранить квартиру
     */
    suspend fun save(flat: Flat): Flat = withContext(Dispatchers.IO) {
        if (flat.id == null) {
            entityManager.persist(flat)
            flat
        } else {
            entityManager.merge(flat)
        }
    }
    
    /**
     * Удалить квартиру по ID
     */
    suspend fun deleteById(id: Long): Boolean = withContext(Dispatchers.IO) {
        val flat = findById(id)
        if (flat != null) {
            entityManager.remove(flat)
            true
        } else {
            false
        }
    }
    
    /**
     * Найти квартиры по названию (содержит подстроку)
     */
    suspend fun findByNameContaining(substring: String): List<Flat> = withContext(Dispatchers.IO) {
        entityManager.createQuery(
            "SELECT f FROM Flat f WHERE LOWER(f.name) LIKE LOWER(:substring)",
            Flat::class.java
        )
            .setParameter("substring", "%$substring%")
            .resultList
    }
    
    /**
     * Подсчитать квартиры с количеством комнат больше заданного
     */
    suspend fun countByNumberOfRoomsGreaterThan(minRooms: Int): Long = withContext(Dispatchers.IO) {
        entityManager.createQuery(
            "SELECT COUNT(f) FROM Flat f WHERE f.numberOfRooms > :minRooms",
            Long::class.java
        )
            .setParameter("minRooms", minRooms)
            .singleResult
    }
    
    /**
     * Найти квартиры с жилой площадью меньше заданной
     */
    suspend fun findByLivingSpaceLessThan(maxSpace: Long): List<Flat> = withContext(Dispatchers.IO) {
        entityManager.createQuery(
            "SELECT f FROM Flat f WHERE f.livingSpace < :maxSpace",
            Flat::class.java
        )
            .setParameter("maxSpace", maxSpace)
            .resultList
    }
    
    /**
     * Найти самую дешевую квартиру с балконом
     */
    suspend fun findCheapestWithBalcony(): Flat? = withContext(Dispatchers.IO) {
        val results = entityManager.createQuery(
            "SELECT f FROM Flat f WHERE f.balcony = true ORDER BY f.price ASC",
            Flat::class.java
        )
            .setMaxResults(1)
            .resultList
        
        results.firstOrNull()
    }
    
    /**
     * Найти все квартиры, отсортированные по времени до метро
     */
    suspend fun findAllSortedByMetroTime(): List<Flat> = withContext(Dispatchers.IO) {
        entityManager.createQuery(
            "SELECT f FROM Flat f ORDER BY f.timeToMetroOnFoot ASC",
            Flat::class.java
        )
            .resultList
    }
    
    /**
     * Поиск с фильтрацией по различным полям
     */
    suspend fun findWithFilters(
        nameFilter: String? = null,
        minPrice: Long? = null,
        maxPrice: Long? = null,
        hasBalcony: Boolean? = null,
        minRooms: Int? = null,
        maxRooms: Int? = null,
        page: Int = 0,
        size: Int = 20,
        sortBy: String = "id",
        sortDirection: String = "ASC"
    ): List<Flat> = withContext(Dispatchers.IO) {
        
        val queryBuilder = StringBuilder("SELECT f FROM Flat f WHERE 1=1")
        val parameters = mutableMapOf<String, Any>()
        
        nameFilter?.let {
            queryBuilder.append(" AND LOWER(f.name) LIKE LOWER(:nameFilter)")
            parameters["nameFilter"] = "%$it%"
        }
        
        minPrice?.let {
            queryBuilder.append(" AND f.price >= :minPrice")
            parameters["minPrice"] = it
        }
        
        maxPrice?.let {
            queryBuilder.append(" AND f.price <= :maxPrice")
            parameters["maxPrice"] = it
        }
        
        hasBalcony?.let {
            queryBuilder.append(" AND f.balcony = :hasBalcony")
            parameters["hasBalcony"] = it
        }
        
        minRooms?.let {
            queryBuilder.append(" AND f.numberOfRooms >= :minRooms")
            parameters["minRooms"] = it
        }
        
        maxRooms?.let {
            queryBuilder.append(" AND f.numberOfRooms <= :maxRooms")
            parameters["maxRooms"] = it
        }
        
        queryBuilder.append(" ORDER BY f.$sortBy $sortDirection")
        
        val query = entityManager.createQuery(queryBuilder.toString(), Flat::class.java)
        
        parameters.forEach { (key, value) ->
            query.setParameter(key, value)
        }
        
        query.firstResult = page * size
        query.maxResults = size
        
        query.resultList
    }
}
