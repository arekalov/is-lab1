package com.arekalov.islab1.interceptor;

import jakarta.interceptor.InterceptorBinding;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Аннотация для включения логирования статистики L2 кэша
 * 
 * Использование:
 * @CacheStatistics
 * public Flat findById(Long id) { ... }
 * 
 * При вызове метода будет залогирована статистика кэша (hits/misses/size)
 */
@InterceptorBinding
@Target({TYPE, METHOD})
@Retention(RUNTIME)
public @interface CacheStatistics {
}

