package com.rumi.iam.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * The Firebase token of a device of a {@link User}, kept so that Notification can push alerts to it (US14).
 * It belongs to the User aggregate. The token is stored as is because Firebase needs it back; it is a bearer
 * secret for push only, so it is never printed.
 */
public final class DeviceToken {

    public static final int MAX_TOKEN_LENGTH = 1024;

    private final UUID id;
    private final UUID userId;
    private final String token;
    private final DevicePlatform platform;
    private final Instant registeredAt;
    private final Instant updatedAt;

    public DeviceToken(UUID id, UUID userId, String token, DevicePlatform platform, Instant registeredAt,
                       Instant updatedAt) {
        this.id = Objects.requireNonNull(id, "Device token id is required");
        this.userId = Objects.requireNonNull(userId, "Device token user id is required");
        this.token = requireToken(token);
        this.platform = Objects.requireNonNull(platform, "Device platform is required");
        this.registeredAt = Objects.requireNonNull(registeredAt, "Device registration time is required");
        this.updatedAt = Objects.requireNonNull(updatedAt, "Device update time is required");
    }

    public static DeviceToken register(UUID id, UUID userId, String token, DevicePlatform platform, Instant now) {
        return new DeviceToken(id, userId, token, platform, now, now);
    }

    /**
     * Returns the same device registered again by {@code userId}. When that is another user (the phone changed
     * account), the device moves to it.
     */
    public DeviceToken refreshFor(UUID userId, DevicePlatform platform, Instant now) {
        return new DeviceToken(id, userId, token, platform, registeredAt, now);
    }

    public boolean belongsTo(UUID userId) {
        return this.userId.equals(userId);
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getToken() {
        return token;
    }

    public DevicePlatform getPlatform() {
        return platform;
    }

    public Instant getRegisteredAt() {
        return registeredAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public String toString() {
        return "DeviceToken[id=" + id + ", userId=" + userId + ", platform=" + platform + "]";
    }

    private static String requireToken(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Device token is required");
        }
        if (token.length() > MAX_TOKEN_LENGTH) {
            throw new IllegalArgumentException("Device token must have at most " + MAX_TOKEN_LENGTH + " characters");
        }
        return token;
    }
}
