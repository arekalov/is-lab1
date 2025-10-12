package com.arekalov.islab1.exception;

import com.arekalov.islab1.dto.response.ErrorResponse;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.util.stream.Collectors;

@Provider
public class ValidationExceptionMapper implements ExceptionMapper<ConstraintViolationException> {
    
    @Override
    public Response toResponse(ConstraintViolationException e) {
        String message = e.getConstraintViolations().stream()
            .map(violation -> {
                String path = violation.getPropertyPath().toString();
                String field = path.substring(path.lastIndexOf('.') + 1);
                return String.format("%s: %s", field, violation.getMessage());
            })
            .collect(Collectors.joining(", "));
        
        return Response
            .status(Response.Status.BAD_REQUEST)
            .entity(new ErrorResponse(message))
            .build();
    }
}
