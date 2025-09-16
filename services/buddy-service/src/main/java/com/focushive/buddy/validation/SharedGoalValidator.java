package com.focushive.buddy.validation;

import com.focushive.buddy.dto.GoalCreationDto;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validator to ensure partnership ID is provided for shared goals
 */
public class SharedGoalValidator implements ConstraintValidator<ValidSharedGoal, GoalCreationDto> {

    @Override
    public void initialize(ValidSharedGoal constraintAnnotation) {
        // No initialization needed
    }

    @Override
    public boolean isValid(GoalCreationDto dto, ConstraintValidatorContext context) {
        if (dto == null) {
            return true; // Let @NotNull handle null cases
        }

        // If goal type is SHARED, partnership ID must be present
        if (dto.getGoalType() == GoalCreationDto.GoalType.SHARED && dto.getPartnershipId() == null) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("Partnership ID is required for shared goals")
                   .addPropertyNode("partnershipId")
                   .addConstraintViolation();
            return false;
        }

        // If goal type is INDIVIDUAL, partnership ID should be null
        if (dto.getGoalType() == GoalCreationDto.GoalType.INDIVIDUAL && dto.getPartnershipId() != null) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("Partnership ID should not be provided for individual goals")
                   .addPropertyNode("partnershipId")
                   .addConstraintViolation();
            return false;
        }

        return true;
    }
}