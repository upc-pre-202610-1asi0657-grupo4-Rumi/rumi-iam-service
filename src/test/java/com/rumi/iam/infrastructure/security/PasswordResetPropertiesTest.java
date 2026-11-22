package com.rumi.iam.infrastructure.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PasswordResetPropertiesTest {

    private static final String BASE_URL = "http://localhost:4200/reset-password";

    @Test
    void linksAreValidForThirtyMinutesByDefault() {
        assertThat(new PasswordResetProperties(BASE_URL, null).ttl()).isEqualTo(Duration.ofMinutes(30));
    }

    @Test
    void keepsAConfiguredLifetime() {
        assertThat(new PasswordResetProperties(BASE_URL, Duration.ofMinutes(10)).ttl()).isEqualTo(Duration.ofMinutes(10));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void refusesToStartWithoutABaseUrl(String baseUrl) {
        assertThatThrownBy(() -> new PasswordResetProperties(baseUrl, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("rumi.password-reset.base-url is not set");
    }

    @Test
    void refusesANonPositiveLifetime() {
        assertThatThrownBy(() -> new PasswordResetProperties(BASE_URL, Duration.ZERO))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("rumi.password-reset.ttl must be a positive duration");
    }
}
