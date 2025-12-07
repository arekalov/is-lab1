package com.arekalov.islab1.config;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

/**
 * Producer для Validator
 */
@ApplicationScoped
public class ValidatorProducer {
    
    private final ValidatorFactory validatorFactory;
    
    public ValidatorProducer() {
        this.validatorFactory = Validation.buildDefaultValidatorFactory();
    }
    
    @Produces
    @ApplicationScoped
    public Validator getValidator() {
        return validatorFactory.getValidator();
    }
}

