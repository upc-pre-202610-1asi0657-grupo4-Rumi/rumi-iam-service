package com.rumi.iam.domain.model;

import com.rumi.iam.UserFixtures;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PasswordResetTokenTest {

    private static final UUID TOKEN_ID = UUID.fromString("0b9e3c52-8f1d-4a6b-9c7e-2d4f6a8b0c1e");
    private static final String TOKEN_HASH = "a".repeat(64);
    private static final Instant NOW = UserFixtures.CREATED_AT;
    private static final Duration TTL = Duration.ofMinutes(30);

    private static PasswordResetToken issued() {
        return PasswordResetToken.issue(TOKEN_ID, UserFixtures.USER_ID, TOKEN_HASH, NOW, TTL);
    }

    @Test
    void isIssuedForAUserWithAnExpirationAndUnused() {
        PasswordResetToken token = issued();

        assertThat(token.getId()).isEqualTo(TOKEN_ID);
        assertThat(token.getUserId()).isEqualTo(UserFixtures.USER_ID);
        assertThat(token.getTokenHash()).isEqualTo(TOKEN_HASH);
        assertThat(token.getCreatedAt()).isEqualTo(NOW);
        assertThat(token.getExpiresAt()).isEqualTo(NOW.plus(TTL));
        assertThat(token.getUsedAt()).isNull();
    }

    @Test
    void isUsableBeforeItExpires() {
        assertThat(issued().isUsable(NOW)).isTrue();
        assertThat(issued().isUsable(NOW.plus(TTL).minusSeconds(1))).isTrue();
    }

    @Test
    void isNotUsableOnceThirtyMinutesHavePassed() {
        assertThat(issued().isUsable(NOW.plus(TTL))).isFalse();
        assertThat(issued().isUsable(NOW.plus(Duration.ofMinutes(31)))).isFalse();
    }

    @Test
    void isNotUsableOnceUsed() {
        PasswordResetToken used = issued().markUsed(NOW.plusSeconds(60));

        assertThat(used.getUsedAt()).isEqualTo(NOW.plusSeconds(60));
        assertThat(used.isUsable(NOW.plusSeconds(61))).isFalse();
        assertThat(used.getId()).isEqualTo(TOKEN_ID);
        assertThat(used.getExpiresAt()).isEqualTo(NOW.plus(TTL));
    }

    @Test
    void cannotBeMarkedUsedTwice() {
        PasswordResetToken used = issued().markUsed(NOW);

        assertThatThrownBy(() -> used.markUsed(NOW.plusSeconds(1)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Password reset token was already used");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"abc", "ZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZ"})
    void rejectsAHashThatIsNotSixtyFourHexCharacters(String hash) {
        assertThatThrownBy(() -> PasswordResetToken.issue(TOKEN_ID, UserFixtures.USER_ID, hash, NOW, TTL))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Token hash must be a SHA-256 in hexadecimal");
    }

    @Test
    void rejectsANonPositiveLifetime() {
        assertThatThrownBy(() -> PasswordResetToken.issue(TOKEN_ID, UserFixtures.USER_ID, TOKEN_HASH, NOW, Duration.ZERO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Token lifetime must be positive");
    }

    @Test
    void doesNotPrintItsHash() {
        assertThat(issued().toString()).doesNotContain(TOKEN_HASH);
    }
}
