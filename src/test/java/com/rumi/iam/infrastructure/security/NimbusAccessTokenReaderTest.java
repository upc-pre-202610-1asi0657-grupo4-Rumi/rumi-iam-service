package com.rumi.iam.infrastructure.security;

import com.rumi.iam.MutableClock;
import com.rumi.iam.UserFixtures;
import com.rumi.iam.domain.service.AccessTokenClaims;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class NimbusAccessTokenReaderTest {

    private static final String SECRET = "test-only-secret-not-for-production-32b!";
    private static final Instant NOW = Instant.parse("2026-11-28T06:00:00Z");

    private final MutableClock clock = new MutableClock(NOW);
    private final JwtSecurityProperties properties = new JwtSecurityProperties(SECRET, Duration.ofMinutes(15));
    private final NimbusJwtTokenIssuer issuer = new NimbusJwtTokenIssuer(properties, clock);
    private final NimbusAccessTokenReader reader = new NimbusAccessTokenReader(properties, clock);

    @Test
    void readsTheUserAndTheAuthTimeOfATokenItIssued() {
        Instant signedInAt = NOW.minus(Duration.ofHours(2));
        String token = issuer.issue(UserFixtures.administrator(), signedInAt).value();

        assertThat(reader.read(token)).contains(new AccessTokenClaims(UserFixtures.USER_ID, signedInAt));
    }

    @Test
    void rejectsATokenSignedWithAnotherKey() {
        NimbusJwtTokenIssuer other = new NimbusJwtTokenIssuer(
                new JwtSecurityProperties("another-test-only-secret-the-reader-does-not-know", null), clock);

        assertThat(reader.read(other.issue(UserFixtures.administrator(), NOW).value())).isEmpty();
    }

    @Test
    void rejectsAnExpiredTokenWithoutClockSkew() {
        String token = issuer.issue(UserFixtures.administrator(), NOW).value();
        clock.advance(Duration.ofMinutes(15).plusSeconds(1));

        assertThat(reader.read(token)).isEmpty();
    }

    @Test
    void rejectsSomethingThatIsNotAToken() {
        assertThat(reader.read("header.payload.signature")).isEmpty();
        assertThat(reader.read("")).isEmpty();
        assertThat(reader.read(null)).isEmpty();
    }
}
