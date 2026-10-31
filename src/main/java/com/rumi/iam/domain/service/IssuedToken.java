package com.rumi.iam.domain.service;

import java.time.Instant;
import java.util.Objects;

public record IssuedToken(String value, Instant expiresAt) {

    public IssuedToken {
        Objects.requireNonNull(value, "Token value is required");
        Objects.requireNonNull(expiresAt, "Token expiration is required");
    }

    /**
     * Never prints the token itself, so that it cannot leak into logs.
     */
    @Override
    public String toString() {
        return "IssuedToken[expiresAt=" + expiresAt + "]";
    }
}
