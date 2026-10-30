package com.rumi.iam.infrastructure.security;

import java.time.Duration;

public record JwtSecurityProperties(String secret, Duration expiration) {

    static final int MINIMUM_SECRET_BYTES = 32;
    static final Duration DEFAULT_EXPIRATION = Duration.ofHours(1);

    public JwtSecurityProperties {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public byte[] secretBytes() {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
