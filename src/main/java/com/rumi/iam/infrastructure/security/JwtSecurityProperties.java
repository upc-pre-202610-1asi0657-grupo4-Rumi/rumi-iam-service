package com.rumi.iam.infrastructure.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * HS256 secret shared with rumi-api-gateway, read from {@code JWT_SECRET}, and the token lifetime, read from
 * {@code JWT_EXPIRATION}. The service refuses to start without a secret of at least 32 bytes, as the gateway does.
 */
@ConfigurationProperties(prefix = "rumi.security.jwt")
public record JwtSecurityProperties(String secret, Duration expiration) {

    static final int MINIMUM_SECRET_BYTES = 32;
    static final Duration DEFAULT_EXPIRATION = Duration.ofMinutes(15);

    public JwtSecurityProperties {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("JWT_SECRET is not set: the service cannot issue tokens without it");
        }
        if (secret.getBytes(StandardCharsets.UTF_8).length < MINIMUM_SECRET_BYTES) {
            throw new IllegalStateException("JWT_SECRET must be at least " + MINIMUM_SECRET_BYTES + " bytes long");
        }
        if (expiration == null) {
            expiration = DEFAULT_EXPIRATION;
        }
        if (expiration.isNegative() || expiration.isZero()) {
            throw new IllegalStateException("JWT_EXPIRATION must be a positive duration");
        }
    }

    public byte[] secretBytes() {
        return secret.getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Never prints the secret, so that it cannot leak into logs.
     */
    @Override
    public String toString() {
        return "JwtSecurityProperties[expiration=" + expiration + "]";
    }
}
