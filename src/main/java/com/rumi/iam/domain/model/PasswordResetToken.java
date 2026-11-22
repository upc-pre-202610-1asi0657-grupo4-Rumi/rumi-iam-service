package com.rumi.iam.domain.model;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * A single-use permission to reset the password of a {@link User} (US03). It belongs to the User aggregate.
 * Only the SHA-256 of the token is held; the raw token travels only in the link sent to the user.
 */
public final class PasswordResetToken {

    private static final Pattern SHA_256_HEX = Pattern.compile("^[0-9a-f]{64}$");

    private final UUID id;
    private final UUID userId;
    private final String tokenHash;
    private final Instant expiresAt;
    private final Instant usedAt;
    private final Instant createdAt;

    public PasswordResetToken(UUID id, UUID userId, String tokenHash, Instant expiresAt, Instant usedAt,
                              Instant createdAt) {
        this.id = Objects.requireNonNull(id, "Token id is required");
        this.userId = Objects.requireNonNull(userId, "Token user id is required");
        this.tokenHash = requireHash(tokenHash);
        this.expiresAt = Objects.requireNonNull(expiresAt, "Token expiration is required");
        this.usedAt = usedAt;
        this.createdAt = Objects.requireNonNull(createdAt, "Token creation time is required");
    }

    /**
     * Issues a new, unused token that expires {@code lifetime} after {@code now}.
     */
    public static PasswordResetToken issue(UUID id, UUID userId, String tokenHash, Instant now, Duration lifetime) {
        Objects.requireNonNull(now, "Token creation time is required");
        if (lifetime == null || lifetime.isNegative() || lifetime.isZero()) {
            throw new IllegalArgumentException("Token lifetime must be positive");
        }
        return new PasswordResetToken(id, userId, tokenHash, now.plus(lifetime), null, now);
    }

    /**
     * A token can reset the password while it is unused and its expiration has not been reached.
     */
    public boolean isUsable(Instant now) {
        return usedAt == null && now.isBefore(expiresAt);
    }

    /**
     * Returns the same token marked as used at {@code now}. A token is used only once.
     */
    public PasswordResetToken markUsed(Instant now) {
        if (usedAt != null) {
            throw new IllegalStateException("Password reset token was already used");
        }
        return new PasswordResetToken(id, userId, tokenHash, expiresAt, Objects.requireNonNull(now), createdAt);
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getUsedAt() {
        return usedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    @Override
    public String toString() {
        return "PasswordResetToken[id=" + id + ", userId=" + userId + ", expiresAt=" + expiresAt
                + ", used=" + (usedAt != null) + "]";
    }

    private static String requireHash(String tokenHash) {
        if (tokenHash == null || !SHA_256_HEX.matcher(tokenHash).matches()) {
            throw new IllegalArgumentException("Token hash must be a SHA-256 in hexadecimal");
        }
        return tokenHash;
    }
}
