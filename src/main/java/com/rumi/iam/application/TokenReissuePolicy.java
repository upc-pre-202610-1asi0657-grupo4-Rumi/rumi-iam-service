package com.rumi.iam.application;

import java.time.Duration;
import java.util.Objects;

/**
 * How long after the password sign-in a token can still be reissued. After that the user signs in again.
 */
public record TokenReissuePolicy(Duration maxSessionAge) {

    public TokenReissuePolicy {
        Objects.requireNonNull(maxSessionAge, "Maximum session age is required");
        if (maxSessionAge.isNegative() || maxSessionAge.isZero()) {
            throw new IllegalArgumentException("Maximum session age must be positive");
        }
    }
}
