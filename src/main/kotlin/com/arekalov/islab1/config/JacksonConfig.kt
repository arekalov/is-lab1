package com.arekalov.islab1.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.inject.Produces
import jakarta.inject.Singleton

/**
 * Конфигурация Jackson для JSON сериализации
 */
@ApplicationScoped
class JacksonConfig {
    
    @Produces
    @Singleton
    fun objectMapper(): ObjectMapper {
        return ObjectMapper().apply {
            // Поддержка Kotlin
            registerModule(KotlinModule.Builder().build())
            
            // Поддержка Java Time API
            registerModule(JavaTimeModule())
            
            // Настройки сериализации
            disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            enable(SerializationFeature.INDENT_OUTPUT)
        }
    }
}
