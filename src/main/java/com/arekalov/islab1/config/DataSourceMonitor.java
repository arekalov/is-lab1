package com.arekalov.islab1.config;

import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Мониторинг статистики DBCP2 пула соединений
 * Периодически логирует статистику пула
 */
@ApplicationScoped
public class DataSourceMonitor {
    
    private static final Logger logger = Logger.getLogger(DataSourceMonitor.class.getName());
    
    @Inject
    private DataSourceConfig dataSourceConfig;
    
    private ScheduledExecutorService scheduler;
    
    /**
     * Запускает периодическое логирование статистики пула
     */
    public void startMonitoring() {
        if (scheduler == null || scheduler.isShutdown()) {
            logger.info("Starting DBCP2 DataSource monitoring...");
            scheduler = Executors.newSingleThreadScheduledExecutor();
            scheduler.scheduleAtFixedRate(() -> {
                dataSourceConfig.logPoolStatistics();
            }, 60, 60, TimeUnit.SECONDS); // Логируем каждые 60 секунд
        }
    }
    
    /**
     * Останавливает мониторинг при остановке приложения
     */
    @PreDestroy
    public void stopMonitoring() {
        if (scheduler != null && !scheduler.isShutdown()) {
            logger.info("Stopping DBCP2 DataSource monitoring...");
            scheduler.shutdownNow();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    logger.warning("DBCP2 DataSource monitor did not terminate in time.");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.warning("DBCP2 DataSource monitor shutdown interrupted.");
            }
        }
    }
}

