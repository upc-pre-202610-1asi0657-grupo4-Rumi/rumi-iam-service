package com.rumi.iam.infrastructure.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtSecurityPropertiesTest {

    private static final String VALID_SECRET = "x".repeat(32);

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void refusesToStartWithoutASecret(String secret) {
        assertThatThrownBy(() -> new JwtSecurityProperties(secret, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("JWT_SECRET is not set: the service cannot issue tokens without it");
    }

    @Test
    void refusesASecretShorterThanThirtyTwoBytes() {
        assertThatThrownBy(() -> new JwtSecurityProperties("x".repeat(31), null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("JWT_SECRET must be at least 32 bytes long");
    }

    @Test
    void countsTheSecretInUtf8Bytes() {
        // Sixteen "ñ" are 32 bytes, the same key length the gateway checks.
        assertThat(new JwtSecurityProperties("ñ".repeat(16), null).secretBytes()).hasSize(32);
    }

    @Test
    void neverPrintsTheSecret() {
        assertThat(new JwtSecurityProperties(VALID_SECRET, null).toString()).doesNotContain(VALID_SECRET);
    }

    @Test
    void expiresTokensAfterFifteenMinutesByDefault() {
        assertThat(new JwtSecurityProperties(VALID_SECRET, null).expiration()).isEqualTo(Duration.ofMinutes(15));
    }

    @Test
    void acceptsAConfiguredExpiration() {
        assertThat(new JwtSecurityProperties(VALID_SECRET, Duration.ofMinutes(30)).expiration())
                .isEqualTo(Duration.ofMinutes(30));
    }

    @Test
    void refusesAnExpirationThatIsNotPositive() {
        assertThatThrownBy(() -> new JwtSecurityProperties(VALID_SECRET, Duration.ZERO))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("JWT_EXPIRATION must be a positive duration");
    }
}
