package com.rumi.iam.infrastructure.web.validation;

import com.rumi.iam.domain.model.PasswordPolicy;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The password follows {@link PasswordPolicy}: at least 8 characters and at most 72 bytes in UTF-8.
 * A null password is left to {@code @NotNull}.
 */
@Documented
@Constraint(validatedBy = PasswordLengthValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface PasswordLength {

    String message() default PasswordPolicy.MESSAGE;

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
