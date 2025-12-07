package com.arekalov.islab1.exception;

/**
 * Исключение, выбрасываемое при нарушении ограничений уникальности бизнес-логики
 */
public class UniqueConstraintViolationException extends RuntimeException {
    
    public UniqueConstraintViolationException(String message) {
        super(message);
    }
    
    public UniqueConstraintViolationException(String message, Throwable cause) {
        super(message, cause);
    }
}

