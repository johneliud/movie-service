package io.github.johneliud.movie_service.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = MaxCurrentYearPlusValidator.class)
public @interface MaxCurrentYearPlus {

    int years() default 10;

    String message() default "Release year must not exceed {years} years from the current year";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}