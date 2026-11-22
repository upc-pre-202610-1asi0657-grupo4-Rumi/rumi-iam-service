package com.rumi.iam.domain.service;

import java.util.Objects;

/**
 * A new reset token: the raw value that goes only into the link, and its hash, which is the only part stored.
 */
public record GeneratedResetToken(String raw, String hash) {

    public GeneratedResetToken {
        Objects.requireNonNull(raw, "Raw token is required");
        Objects.requireNonNull(hash, "Token hash is required");
    }

    /**
     * Never prints the raw token, so that it cannot leak into logs.
     */
    @Override
    public String toString() {
        return "GeneratedResetToken[hash=" + hash + "]";
    }
}
