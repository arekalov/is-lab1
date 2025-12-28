package com.arekalov.islab1.config;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import static org.eclipse.persistence.config.PersistenceUnitProperties.NON_JTA_DATASOURCE;

/**
 * Producer для EntityManagerFactory с использованием DBCP2 DataSource
 */
@ApplicationScoped
public class EntityManagerProducer {
    
    private static final Logger logger = Logger.getLogger(EntityManagerProducer.class.getName());
    
    @Inject
    @Named("dbcp2DataSource")
    private DataSource dataSource;
    
    /**
     * Создает EntityManagerFactory с нашим DBCP2 DataSource
     */
    @Produces
    @ApplicationScoped
    public EntityManagerFactory createEntityManagerFactory() {
        logger.info("Creating EntityManagerFactory with DBCP2 DataSource.");
        
        Map<String, Object> properties = new HashMap<>();
        properties.put(NON_JTA_DATASOURCE, dataSource);
        
        return Persistence.createEntityManagerFactory("flatsPU", properties);
    }
}

