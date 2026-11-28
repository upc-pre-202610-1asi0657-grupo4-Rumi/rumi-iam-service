package com.rumi.iam.infrastructure.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * {@code rumi.security.token-reissue.max-session-age}: how long after the password sign-in POST
 * /api/v1/users/me/token still issues a new token (12 hours by default).
 */
@ConfigurationProperties(prefix = "rumi.security.token-reissue")
public record TokenReissueProperties(Duration maxSessionAge) {

    static final Duration DEFAULT_MAX_SESSION_AGE = Duration.ofHours(12);

    public TokenReissueProperties {
        if (maxSessionAge == null) {
            maxSessionAge = DEFAULT_MAX_SESSION_AGE;
        }
        if (maxSessionAge.isNegative() || maxSessionAge.isZero()) {
            throw new IllegalStateException("rumi.security.token-reissue.max-session-age must be a positive duration");
        }
    }
}
