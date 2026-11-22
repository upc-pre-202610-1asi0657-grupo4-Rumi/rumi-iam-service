package com.rumi.iam.infrastructure.security;

import com.rumi.iam.domain.service.GeneratedResetToken;
import com.rumi.iam.domain.service.ResetTokenGenerator;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/**
 * 32 random bytes from {@link SecureRandom}, encoded as URL-safe Base64 without padding, and their SHA-256 in
 * lowercase hexadecimal. A plain hash is enough here: the token has 256 bits of entropy, unlike a password.
 */
@Component
public class Sha256ResetTokenGenerator implements ResetTokenGenerator {

    private static final int TOKEN_BYTES = 32;

    private final SecureRandom random = new SecureRandom();

    @Override
    public GeneratedResetToken generate() {
        byte[] bytes = new byte[TOKEN_BYTES];
        random.nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        return new GeneratedResetToken(raw, hash(raw));
    }

    @Override
    public String hash(String rawToken) {
        try {
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(sha256.digest(rawToken.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException unavailable) {
            throw new IllegalStateException("SHA-256 is not available", unavailable);
        }
    }
}
