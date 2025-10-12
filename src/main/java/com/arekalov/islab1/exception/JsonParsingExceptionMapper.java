package com.arekalov.islab1.exception;

import com.arekalov.islab1.dto.response.ErrorResponse;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class JsonParsingExceptionMapper implements ExceptionMapper<Exception> {
    
    @Override
    public Response toResponse(Exception e) {
        String message;
        
        if (e instanceof InvalidFormatException ife) {
            String fieldName = ife.getPath().get(ife.getPath().size() - 1).getFieldName();
            String targetType = ife.getTargetType().getSimpleName().toLowerCase();
            String value = String.valueOf(ife.getValue());
            
            message = String.format(
                "Поле '%s' должно быть типа %s, получено значение: '%s'",
                fieldName,
                targetType,
                value
            );
        } else if (e instanceof JsonParseException) {
            message = "Неверный формат JSON";
        } else if (e instanceof JsonMappingException jme) {
            message = "Ошибка при разборе JSON: " + jme.getOriginalMessage();
        } else {
            message = "Ошибка при обработке запроса: " + e.getMessage();
        }
        
        return Response
            .status(Response.Status.BAD_REQUEST)
            .entity(new ErrorResponse(message))
            .build();
    }
}