package com.rumi.iam.application;

import java.time.Duration;
import java.util.Objects;

/**
 * How long a password reset link works (US03 says 30 minutes). Built from configuration by the infrastructure.
 */
public record PasswordResetPolicy(Duration tokenLifetime) {

    public PasswordResetPolicy {
        Objects.requireNonNull(tokenLifetime, "Token lifetime is required");
    }
}
