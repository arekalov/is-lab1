package com.arekalov.islab1.exception;

/**
 * Исключение для ошибок валидации данных
 */
public class ValidationException extends IllegalArgumentException {
    
    public ValidationException(String message) {
        super(message);
    }
    
    public ValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}

