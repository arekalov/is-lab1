package com.arekalov.islab1.interceptor;

import com.arekalov.islab1.service.CacheStatisticsService;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

import java.util.logging.Logger;

/**
 * CDI Interceptor для логирования статистики L2 кэша
 * 
 * Перехватывает вызовы методов аннотированных @CacheStatistics
 * и логирует cache hits/misses
 * 
 * Пример лога:
 * [CACHE STATS] FlatRepository.findById(id=123) took 15ms
 *   → CACHE HIT (object was in L2 cache)
 * 
 * [CACHE STATS] FlatRepository.findById(id=456) took 85ms
 *   → CACHE MISS (object loaded from database)
 */
@CacheStatistics
@Interceptor
@Priority(Interceptor.Priority.APPLICATION)
public class CacheStatisticsInterceptor {
    
    private static final Logger logger = Logger.getLogger(CacheStatisticsInterceptor.class.getName());
    
    @Inject
    private CacheStatisticsService cacheStatsService;
    
    @AroundInvoke
    public Object logCacheStats(InvocationContext ctx) throws Exception {
        // Если логирование выключено - просто выполняем метод
        if (!cacheStatsService.isEnabled()) {
            return ctx.proceed();
        }
        
        String className = ctx.getTarget().getClass().getSimpleName();
        String methodName = ctx.getMethod().getName();
        
        // Определяем entity class и id для методов findById
        Class<?> entityClass = null;
        Object entityId = null;
        
        if (methodName.contains("findById") && ctx.getParameters().length > 0) {
            // Получаем ID из параметров метода
            entityId = ctx.getParameters()[0];
            
            // Определяем тип entity по имени класса Repository
            entityClass = guessEntityClass(className);
        }
        
        // Проверяем был ли объект в кэше ДО вызова
        boolean wasInCacheBefore = false;
        if (entityClass != null && entityId != null) {
            wasInCacheBefore = cacheStatsService.isInCache(entityClass, entityId);
        }
        
        // Замеряем время выполнения
        long startTime = System.currentTimeMillis();
        
        // Выполняем метод
        Object result = ctx.proceed();
        
        long duration = System.currentTimeMillis() - startTime;
        
        // Проверяем есть ли объект в кэше ПОСЛЕ вызова
        boolean isInCacheAfter = false;
        if (entityClass != null && entityId != null) {
            isInCacheAfter = cacheStatsService.isInCache(entityClass, entityId);
        }
        
        // Логируем результат
        logResult(className, methodName, entityId, duration, wasInCacheBefore, isInCacheAfter, result);
        
        return result;
    }
    
    /**
     * Логирование результата с определением cache hit/miss
     */
    private void logResult(String className, String methodName, Object entityId, 
                          long duration, boolean wasInCacheBefore, boolean isInCacheAfter, 
                          Object result) {
        
        StringBuilder logMessage = new StringBuilder();
        logMessage.append("\n╔═══ [CACHE STATS] ═══════════════════════════════╗\n");
        logMessage.append(String.format("║ %s.%s", className, methodName));
        
        if (entityId != null) {
            logMessage.append(String.format("(id=%s)", entityId));
        }
        logMessage.append("()\n");
        logMessage.append(String.format("║ Duration: %dms\n", duration));
        
        // Определяем cache hit или miss
        if (entityId != null) {
            if (wasInCacheBefore) {
                // Объект был в кэше ДО вызова → CACHE HIT
                logMessage.append("║\n");
                logMessage.append("║ ✓ CACHE HIT - object was already in L2 cache\n");
                logMessage.append("║   (Fast retrieval from memory)\n");
            } else if (isInCacheAfter && result != null) {
                // Объекта не было, но появился ПОСЛЕ → CACHE MISS + loaded from DB
                logMessage.append("║\n");
                logMessage.append("║ ✗ CACHE MISS - object loaded from database\n");
                logMessage.append("║   (Now cached in L2 for future requests)\n");
            } else if (result == null) {
                // Объект не найден вообще
                logMessage.append("║\n");
                logMessage.append("║ ø OBJECT NOT FOUND - not in cache, not in database\n");
            }
        } else {
            // Метод без ID (например findAll)
            logMessage.append("║\n");
            logMessage.append("║ Collection query - individual objects may use cache\n");
        }
        
        logMessage.append("╚══════════════════════════════════════════════════╝");
        
        logger.info(logMessage.toString());
    }
    
    /**
     * Определить класс Entity по имени Repository
     */
    private Class<?> guessEntityClass(String repositoryClassName) {
        try {
            // Убираем CDI proxy суффиксы: $Proxy$_$$_WeldSubclass и т.д.
            String cleanClassName = repositoryClassName;
            if (cleanClassName.contains("$")) {
                cleanClassName = cleanClassName.substring(0, cleanClassName.indexOf("$"));
            }
            
            // Извлекаем имя entity (FlatRepository -> Flat)
            String entityName = cleanClassName.replace("Repository", "");
            String fullClassName = "com.arekalov.islab1.entity." + entityName;
            return Class.forName(fullClassName);
        } catch (ClassNotFoundException e) {
            logger.warning("Could not determine entity class from: " + repositoryClassName);
            return null;
        }
    }
}
