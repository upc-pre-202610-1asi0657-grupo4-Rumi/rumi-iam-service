package com.rumi.iam.infrastructure.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Page of the frontend that opens the reset links ({@code PASSWORD_RESET_BASE_URL}) and how long a link works
 * ({@code PASSWORD_RESET_TTL}, 30 minutes by default as US03 requires).
 */
@ConfigurationProperties(prefix = "rumi.password-reset")
public record PasswordResetProperties(String baseUrl, Duration ttl) {

    static final Duration DEFAULT_TTL = Duration.ofMinutes(30);

    public PasswordResetProperties {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalStateException("rumi.password-reset.base-url is not set");
        }
        if (ttl == null) {
            ttl = DEFAULT_TTL;
        }
        if (ttl.isNegative() || ttl.isZero()) {
            throw new IllegalStateException("rumi.password-reset.ttl must be a positive duration");
        }
    }
}
