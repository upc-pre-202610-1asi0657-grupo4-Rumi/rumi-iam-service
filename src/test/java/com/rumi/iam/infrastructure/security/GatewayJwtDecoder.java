package com.rumi.iam.infrastructure.security;

import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

/**
 * Decodes tokens as rumi-api-gateway's {@code JwtDecoderConfiguration} does (HS256, the shared secret as UTF-8
 * bytes, the default validators and the Rumi claims validator), with the blocking decoder instead of the
 * reactive one.
 */
public final class GatewayJwtDecoder {

    private GatewayJwtDecoder() {
    }

    public static JwtDecoder create(String secret) {
        SecretKeySpec key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefault(), new GatewayRumiClaimsValidator()));
        return decoder;
    }
}
