package com.arekalov.islab1.config

import jakarta.ws.rs.ApplicationPath
import jakarta.ws.rs.core.Application

/**
 * Конфигурация JAX-RS приложения
 */
@ApplicationPath("/api")
class RestApplication : Application()
