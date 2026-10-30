package com.rumi.iam.infrastructure.security;

import com.rumi.iam.UserFixtures;
import com.rumi.iam.domain.model.EmailAddress;
import com.rumi.iam.domain.model.Role;
import com.rumi.iam.domain.model.User;
import com.rumi.iam.domain.service.IssuedToken;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Contract test with rumi-api-gateway: issued tokens are decoded exactly as the gateway's
 * {@code JwtDecoderConfiguration} does (HS256, the shared secret as UTF-8 bytes, the default validators
 * and the Rumi claims validator).
 */
@Disabled("Enabled by the next feat commit")
class GatewayTokenCompatibilityTest {

    private static final String SECRET = "test-only-secret-not-for-production-32b!";
    private static final String OTHER_SECRET = "another-test-only-secret-the-gateway-does-not-know";

    private final Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);

    @Test
    void theGatewayAcceptsTheTokenOfANewAdministrator() {
        User administrator = UserFixtures.administrator();

        IssuedToken token = issuer(SECRET, now).issue(administrator);
        Jwt jwt = gatewayDecoder(SECRET).decode(token.value());

        assertThat(jwt.getHeaders()).containsEntry("alg", "HS256");
        assertThat(jwt.getSubject()).isEqualTo(UserFixtures.USER_ID.toString());
        assertThat(jwt.getClaimAsString("role")).isEqualTo("ADMINISTRATOR");
        assertThat(jwt.hasClaim("buildingIds")).as("buildingIds must be present, even when empty").isTrue();
        assertThat(jwt.getClaimAsStringList("buildingIds")).isEmpty();
        assertThat(jwt.getIssuedAt()).isEqualTo(now);
        assertThat(jwt.getExpiresAt()).isEqualTo(now.plus(Duration.ofHours(1)));
        assertThat(token.expiresAt()).isEqualTo(jwt.getExpiresAt());
    }

    @Test
    void theGatewayAcceptsTheBuildingIdsOfAResident() {
        UUID building = UUID.fromString("3f2c8a10-6b7d-4e9f-a1b2-c3d4e5f60718");
        User resident = new User(UUID.randomUUID(), new EmailAddress("luis@rumi.pe"), "Luis Quispe",
                UserFixtures.PASSWORD_HASH, Role.RESIDENT, Set.of(building), UserFixtures.CREATED_AT);

        Jwt jwt = gatewayDecoder(SECRET).decode(issuer(SECRET, now).issue(resident).value());

        assertThat(jwt.getClaimAsString("role")).isEqualTo("RESIDENT");
        assertThat(jwt.getClaimAsStringList("buildingIds")).containsExactly(building.toString());
    }

    @Test
    void theTokenCarriesNoPersonalDataBeyondTheContract() {
        Jwt jwt = gatewayDecoder(SECRET).decode(issuer(SECRET, now).issue(UserFixtures.administrator()).value());

        assertThat(jwt.getClaims().keySet()).containsExactlyInAnyOrder("sub", "role", "buildingIds", "iat", "exp");
    }

    @Test
    void theGatewayRejectsATokenSignedWithAnotherSecret() {
        String token = issuer(OTHER_SECRET, now).issue(UserFixtures.administrator()).value();

        assertThatThrownBy(() -> gatewayDecoder(SECRET).decode(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void theGatewayRejectsAnExpiredToken() {
        String token = issuer(SECRET, now.minus(Duration.ofHours(2))).issue(UserFixtures.administrator()).value();

        assertThatThrownBy(() -> gatewayDecoder(SECRET).decode(token)).isInstanceOf(JwtValidationException.class);
    }

    @Test
    void usesTheConfiguredExpiration() {
        JwtSecurityProperties properties = new JwtSecurityProperties(SECRET, Duration.ofMinutes(15));
        NimbusJwtTokenIssuer issuer = new NimbusJwtTokenIssuer(properties, Clock.fixed(now, ZoneOffset.UTC));

        assertThat(issuer.issue(UserFixtures.administrator()).expiresAt()).isEqualTo(now.plus(Duration.ofMinutes(15)));
    }

    private static NimbusJwtTokenIssuer issuer(String secret, Instant instant) {
        return new NimbusJwtTokenIssuer(new JwtSecurityProperties(secret, null), Clock.fixed(instant, ZoneOffset.UTC));
    }

    /**
     * Built as in the gateway's JwtDecoderConfiguration, with the blocking decoder instead of the reactive one.
     */
    private static JwtDecoder gatewayDecoder(String secret) {
        SecretKeySpec key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefault(), new GatewayRumiClaimsValidator()));
        return decoder;
    }
}
