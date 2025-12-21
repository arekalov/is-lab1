package com.arekalov.islab1.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.Cache;

import java.util.logging.Logger;

/**
 * Сервис для управления логированием статистики L2 кэша
 * 
 * Включение/выключение через константу CACHE_STATS_ENABLED
 */
@ApplicationScoped
public class CacheStatisticsService {
    
    private static final Logger logger = Logger.getLogger(CacheStatisticsService.class.getName());
    
    private static final boolean CACHE_STATS_ENABLED = true;
    
    @Inject
    private EntityManagerService entityManagerService;
    
    public boolean isEnabled() {
        return CACHE_STATS_ENABLED;
    }
    
    /**
     * Проверить находится ли объект в L2 кэше
     * 
     * @param entityClass класс entity (например, Flat.class)
     * @param id идентификатор объекта
     * @return true если объект в кэше (cache hit), false если нет (cache miss)
     */
    public boolean isInCache(Class<?> entityClass, Object id) {
        try {
            if (id == null) {
                return false;
            }
            
            Cache cache = entityManagerService.getEntityManager()
                .getEntityManagerFactory()
                .getCache();
            
            return cache.contains(entityClass, id);
            
        } catch (Exception e) {
            logger.warning("Failed to check cache: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Очистить весь L2 кэш (для тестирования)
     */
    public void evictAll() {
        try {
            Cache cache = entityManagerService.getEntityManager()
                .getEntityManagerFactory()
                .getCache();
            
            cache.evictAll();
            logger.info("L2 Cache evicted (cleared)");
        } catch (Exception e) {
            logger.severe("Failed to evict cache: " + e.getMessage());
        }
    }
}
