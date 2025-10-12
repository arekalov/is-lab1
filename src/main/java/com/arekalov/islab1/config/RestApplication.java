package com.arekalov.islab1.config;

import com.arekalov.islab1.controller.FlatController;
import com.arekalov.islab1.controller.HouseController;
import com.arekalov.islab1.exception.JsonParsingExceptionMapper;
import com.arekalov.islab1.exception.ValidationExceptionMapper;
import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;
import java.util.Set;

/**
 * Конфигурация JAX-RS приложения
 */
@ApplicationPath("/api")
public class RestApplication extends Application {
    
    @Override
    public Set<Class<?>> getClasses() {
        return Set.of(
            // Контроллеры
            FlatController.class,
            HouseController.class,
            
            // Конфигурация
            JacksonConfig.class,
            
            // Обработчики ошибок
            JsonParsingExceptionMapper.class,
            ValidationExceptionMapper.class
        );
    }
}