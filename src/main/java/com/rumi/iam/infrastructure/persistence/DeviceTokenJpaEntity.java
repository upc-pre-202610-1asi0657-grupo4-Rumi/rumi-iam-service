package com.rumi.iam.infrastructure.persistence;

import com.rumi.iam.domain.model.DevicePlatform;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "device_tokens")
public class DeviceTokenJpaEntity {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false, unique = true, length = 1024)
    private String token;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private DevicePlatform platform;

    @Column(name = "registered_at", nullable = false)
    private Instant registeredAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected DeviceTokenJpaEntity() {
    }

    public DeviceTokenJpaEntity(UUID id, UUID userId, String token, DevicePlatform platform, Instant registeredAt,
                                Instant updatedAt) {
        this.id = id;
        this.userId = userId;
        this.token = token;
        this.platform = platform;
        this.registeredAt = registeredAt;
        this.updatedAt = updatedAt;
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
}
