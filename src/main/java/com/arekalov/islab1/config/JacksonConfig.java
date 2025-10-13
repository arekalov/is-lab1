package com.arekalov.islab1.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Inject;
import jakarta.ws.rs.ext.ContextResolver;
import jakarta.ws.rs.ext.Provider;

@Provider
public class JacksonConfig implements ContextResolver<ObjectMapper> {
    
    @Inject
    private ObjectMapperProducer objectMapperProducer;

    @Override
    public ObjectMapper getContext(Class<?> type) {
        return objectMapperProducer.getObjectMapper();
    }
}