package com.rumi.iam.application;

import com.rumi.iam.domain.service.GeneratedResetToken;
import com.rumi.iam.domain.service.ResetTokenGenerator;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Predictable raw tokens ("raw-token-1", "raw-token-2", ...) with a real SHA-256 hash.
 */
class FakeResetTokenGenerator implements ResetTokenGenerator {

    private int generated;

    @Override
    public GeneratedResetToken generate() {
        generated++;
        String raw = "raw-token-" + generated;
        return new GeneratedResetToken(raw, hash(raw));
    }

    @Override
    public String hash(String rawToken) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }
}
