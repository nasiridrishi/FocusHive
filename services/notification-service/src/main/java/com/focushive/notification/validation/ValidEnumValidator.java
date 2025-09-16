package com.focushive.notification.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Validator implementation for ValidEnum annotation.
 */
public class ValidEnumValidator implements ConstraintValidator<ValidEnum, String> {

    private Set<String> acceptedValues;
    private boolean ignoreCase;
    private String acceptedValuesString;

    @Override
    public void initialize(ValidEnum annotation) {
        this.ignoreCase = annotation.ignoreCase();
        
        // Get all enum constants as strings
        this.acceptedValues = Arrays.stream(annotation.enumClass().getEnumConstants())
                .map(enumConstant -> ignoreCase ? enumConstant.name().toLowerCase() : enumConstant.name())
                .collect(Collectors.toSet());
                
        // Create string representation for error message
        this.acceptedValuesString = Arrays.stream(annotation.enumClass().getEnumConstants())
                .map(Enum::name)
                .collect(Collectors.joining(", "));
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // Null values are handled by @NotNull
        if (value == null) {
            return true;
        }
        
        String valueToCheck = ignoreCase ? value.toLowerCase() : value;
        boolean isValid = acceptedValues.contains(valueToCheck);
        
        if (!isValid) {
            // Customize error message with actual accepted values
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                    "Invalid enum value '" + value + "'. Accepted values are: " + acceptedValuesString
            ).addConstraintViolation();
        }
        
        return isValid;
    }
}