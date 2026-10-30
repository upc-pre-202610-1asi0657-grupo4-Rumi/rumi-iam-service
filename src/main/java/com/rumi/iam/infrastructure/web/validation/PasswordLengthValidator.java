package com.rumi.iam.infrastructure.web.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PasswordLengthValidator implements ConstraintValidator<PasswordLength, String> {

    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
