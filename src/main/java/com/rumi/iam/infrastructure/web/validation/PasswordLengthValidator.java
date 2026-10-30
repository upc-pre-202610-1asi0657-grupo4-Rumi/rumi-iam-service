package com.rumi.iam.infrastructure.web.validation;

import com.rumi.iam.domain.model.PasswordPolicy;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PasswordLengthValidator implements ConstraintValidator<PasswordLength, String> {

    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        return password == null || PasswordPolicy.isValid(password);
    }
}
