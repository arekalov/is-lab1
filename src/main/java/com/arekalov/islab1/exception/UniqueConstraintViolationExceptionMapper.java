package com.arekalov.islab1.exception;

import com.arekalov.islab1.dto.response.ErrorResponse;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.logging.Logger;

/**
 * Обработчик исключений нарушения ограничений уникальности
 */
@Provider
public class UniqueConstraintViolationExceptionMapper implements ExceptionMapper<UniqueConstraintViolationException> {
    
    private static final Logger logger = Logger.getLogger(UniqueConstraintViolationExceptionMapper.class.getName());
    
    @Override
    public Response toResponse(UniqueConstraintViolationException exception) {
        logger.warning("Нарушение ограничения уникальности: " + exception.getMessage());
        
        ErrorResponse errorResponse = new ErrorResponse(exception.getMessage());
        
        return Response
            .status(Response.Status.CONFLICT) // 409 Conflict
            .entity(errorResponse)
            .build();
    }
}

