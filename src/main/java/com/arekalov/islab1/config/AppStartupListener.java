package com.arekalov.islab1.config;

import jakarta.inject.Inject;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import java.util.logging.Logger;

/**
 * Слушатель запуска приложения
 * Инициализирует DBCP2 DataSource и запускает мониторинг
 */
@WebListener
public class AppStartupListener implements ServletContextListener {
    
    private static final Logger logger = Logger.getLogger(AppStartupListener.class.getName());
    
    @Inject
    private DataSourceConfig dataSourceConfig;
    
    @Inject
    private DataSourceMonitor dataSourceMonitor;
    
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        logger.info("Application starting up. Initializing DBCP2 DataSource and monitor.");
        dataSourceConfig.createDataSource(); // Инициализируем DataSource
        dataSourceMonitor.startMonitoring(); // Запускаем мониторинг
    }
    
    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        logger.info("Application shutting down. Stopping DBCP2 DataSource monitor.");
        dataSourceMonitor.stopMonitoring();
        // DataSourceConfig's @PreDestroy закроет DataSource
    }
}

