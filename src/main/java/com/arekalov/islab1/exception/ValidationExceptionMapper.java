package com.arekalov.islab1.exception;

import com.arekalov.islab1.dto.response.ErrorResponse;
import jakarta.ejb.EJBException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.logging.Logger;

/**
 * Обработчик ValidationException для возврата 400 Bad Request
 * Работает с ValidationException, обернутым в EJBException
 */
@Provider
public class ValidationExceptionMapper implements ExceptionMapper<EJBException> {
    
    private static final Logger logger = Logger.getLogger(ValidationExceptionMapper.class.getName());
    
    @Override
    public Response toResponse(EJBException exception) {
        // Извлекаем реальное исключение из EJBException
        Throwable cause = exception.getCause();
        
        if (cause == null) {
            cause = exception;
        }
        
        // Проверяем, является ли причина ValidationException или IllegalArgumentException
        if (cause instanceof ValidationException) {
            logger.warning("ValidationException перехвачена: " + cause.getMessage());
            return Response
                .status(Response.Status.BAD_REQUEST)
                .entity(new ErrorResponse(cause.getMessage()))
                .build();
        }
        
        if (cause instanceof IllegalArgumentException) {
            logger.warning("IllegalArgumentException перехвачена: " + cause.getMessage());
            return Response
                .status(Response.Status.BAD_REQUEST)
                .entity(new ErrorResponse(cause.getMessage()))
                .build();
        }
        
        // Для других EJBException возвращаем 500 с полной информацией
        logger.severe("EJBException: " + exception.getMessage());
        logger.severe("Cause: " + (cause != null ? cause.getClass().getName() + ": " + cause.getMessage() : "null"));
        exception.printStackTrace();
        
        return Response
            .status(Response.Status.INTERNAL_SERVER_ERROR)
            .entity(new ErrorResponse(
                cause != null ? cause.getMessage() : exception.getMessage()
            ))
            .build();
    }
}
