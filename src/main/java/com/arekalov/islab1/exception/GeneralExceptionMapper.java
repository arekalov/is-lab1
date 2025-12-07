package com.arekalov.islab1.exception;

import com.arekalov.islab1.dto.response.ErrorResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.ejb.EJBException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Общий обработчик всех необработанных исключений
 * Возвращает детальную информацию об ошибке
 * НЕ обрабатывает: JsonProcessingException, EJBException (у них есть свои мапперы)
 */
@Provider
public class GeneralExceptionMapper implements ExceptionMapper<Throwable> {
    
    private static final Logger logger = Logger.getLogger(GeneralExceptionMapper.class.getName());
    
    @Override
    public Response toResponse(Throwable exception) {
        // Пропускаем исключения, которые обрабатываются специфичными мапперами
        if (exception instanceof JsonProcessingException) {
            // Будет обработано JsonParsingExceptionMapper
            return null;
        }
        
        if (exception instanceof EJBException) {
            // Будет обработано ValidationExceptionMapper
            return null;
        }
        
        logger.severe("Необработанное исключение: " + exception.getClass().getName() + ": " + exception.getMessage());
        exception.printStackTrace();
        
        // Собираем стек трейс
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        exception.printStackTrace(pw);
        String stackTrace = sw.toString();
        
        // Получаем первые 15 строк стек трейса для details
        List<String> stackLines = Arrays.stream(stackTrace.split("\n"))
            .limit(15)
            .collect(Collectors.toList());
        
        // Формируем детальное сообщение
        String message = String.format("%s: %s", 
            exception.getClass().getSimpleName(),
            exception.getMessage() != null ? exception.getMessage() : "No message"
        );
        
        ErrorResponse errorResponse = new ErrorResponse(message, stackLines);
        
        return Response
            .status(Response.Status.INTERNAL_SERVER_ERROR)
            .entity(errorResponse)
            .build();
    }
}

