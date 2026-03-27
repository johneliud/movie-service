package io.github.johneliud.movie_service.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.time.LocalDate;

public class MaxCurrentYearPlusValidator implements ConstraintValidator<MaxCurrentYearPlus, Integer> {

    private int years;

    @Override
    public void initialize(MaxCurrentYearPlus annotation) {
        this.years = annotation.years();
    }

    @Override
    public boolean isValid(Integer value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        return value <= LocalDate.now().getYear() + years;
    }
}