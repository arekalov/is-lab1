package com.arekalov.islab1.exception;

import com.arekalov.islab1.dto.response.ErrorResponse;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.logging.Logger;

/**
 * Обработчик ошибок парсинга JSON от Jackson
 * Возвращает 400 Bad Request для всех ошибок парсинга/маппинга
 */
@Provider
public class JsonParsingExceptionMapper implements ExceptionMapper<JsonProcessingException> {
    
    private static final Logger logger = Logger.getLogger(JsonParsingExceptionMapper.class.getName());
    
    @Override
    public Response toResponse(JsonProcessingException e) {
        String message;
        
        if (e instanceof InvalidFormatException ife) {
            String fieldName = ife.getPath().isEmpty() ? "unknown" : 
                ife.getPath().get(ife.getPath().size() - 1).getFieldName();
            String targetType = ife.getTargetType().getSimpleName();
            String value = String.valueOf(ife.getValue());
            
            // Особая обработка для enum
            if (ife.getTargetType().isEnum()) {
                Object[] enumConstants = ife.getTargetType().getEnumConstants();
                String validValues = java.util.Arrays.stream(enumConstants)
                    .map(Object::toString)
                    .collect(java.util.stream.Collectors.joining(", "));
                
                message = String.format(
                    "Недопустимое значение '%s' для поля '%s'. Допустимые значения: %s",
                    value,
                    fieldName,
                    validValues
                );
            } else {
            message = String.format(
                "Поле '%s' должно быть типа %s, получено значение: '%s'",
                fieldName,
                targetType,
                value
            );
            }
            
            logger.warning("InvalidFormatException: " + message);
        } else if (e instanceof JsonParseException) {
            message = "Неверный формат JSON: " + e.getOriginalMessage();
            logger.warning("JsonParseException: " + message);
        } else if (e instanceof JsonMappingException jme) {
            message = "Ошибка при разборе JSON: " + jme.getOriginalMessage();
            logger.warning("JsonMappingException: " + message);
        } else {
            message = "Ошибка при обработке JSON: " + e.getMessage();
            logger.warning("JsonProcessingException: " + message);
        }
        
        return Response
            .status(Response.Status.BAD_REQUEST)
            .entity(new ErrorResponse(message))
            .build();
    }
}