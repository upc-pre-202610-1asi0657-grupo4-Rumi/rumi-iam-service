package com.rumi.iam.infrastructure.security;

import com.rumi.iam.domain.service.AccessTokenClaims;
import com.rumi.iam.domain.service.AccessTokenReader;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;

import javax.crypto.spec.SecretKeySpec;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Validates an access token with the same HS256 key that signs it (signature and expiration, no clock skew) and
 * reads its subject and auth_time. The gateway has validated it already; IAM checks again because it is about
 * to issue a new token from it.
 */
@Component
public class NimbusAccessTokenReader implements AccessTokenReader {

    private final NimbusJwtDecoder decoder;

    public NimbusAccessTokenReader(JwtSecurityProperties properties, Clock clock) {
        SecretKeySpec key = new SecretKeySpec(properties.secretBytes(), "HmacSHA256");
        this.decoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
        JwtTimestampValidator expiration = new JwtTimestampValidator(Duration.ZERO);
        expiration.setClock(clock);
        this.decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(expiration));
    }

    @Override
    public Optional<AccessTokenClaims> read(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        try {
            Jwt jwt = decoder.decode(token);
            return Optional.of(new AccessTokenClaims(UUID.fromString(jwt.getSubject()), authTime(jwt)));
        } catch (JwtException | IllegalArgumentException | NullPointerException invalid) {
            return Optional.empty();
        }
    }

    private static Instant authTime(Jwt jwt) {
        Object claim = jwt.getClaims().get(JwtClaimNames.AUTH_TIME);
        if (claim instanceof Number seconds) {
            return Instant.ofEpochSecond(seconds.longValue());
        }
        if (claim instanceof Instant instant) {
            return instant;
        }
        return null;
    }
}
