package com.rumi.iam.infrastructure.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.rumi.iam.domain.model.User;
import com.rumi.iam.domain.service.IssuedToken;
import com.rumi.iam.domain.service.TokenIssuer;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Component;

import javax.crypto.spec.SecretKeySpec;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

/**
 * Issues the HS256 tokens that rumi-api-gateway validates: {@code sub} is the user id, {@code role} a single
 * role name and {@code buildingIds} a list of building ids that is present even when it is empty.
 */
@Component
public class NimbusJwtTokenIssuer implements TokenIssuer {

    private final JwtEncoder encoder;
    private final Duration expiration;
    private final Clock clock;

    public NimbusJwtTokenIssuer(JwtSecurityProperties properties, Clock clock) {
        SecretKeySpec key = new SecretKeySpec(properties.secretBytes(), "HmacSHA256");
        this.encoder = new NimbusJwtEncoder(new ImmutableSecret<>(key));
        this.expiration = properties.expiration();
        this.clock = clock;
    }

    @Override
    public IssuedToken issue(User user) {
        // JWT times are whole seconds; truncating keeps expiresAt equal to the exp claim.
        Instant issuedAt = clock.instant().truncatedTo(ChronoUnit.SECONDS);
        Instant expiresAt = issuedAt.plus(expiration);
        List<String> buildingIds = user.getBuildingIds().stream().map(UUID::toString).sorted().toList();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(user.getId().toString())
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .claim(JwtClaimNames.ROLE, user.getRole().name())
                .claim(JwtClaimNames.BUILDING_IDS, buildingIds)
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();

        String token = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new IssuedToken(token, expiresAt);
    }
}
