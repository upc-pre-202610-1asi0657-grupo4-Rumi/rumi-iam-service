package com.rumi.iam.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * A user of Rumi. Only the hash of the password is ever held here.
 */
public final class User {

    public static final int MAX_FULL_NAME_LENGTH = 100;

    private final UUID id;
    private final EmailAddress email;
    private final String fullName;
    private final String passwordHash;
    private final Role role;
    private final Set<UUID> buildingIds;
    private final Instant createdAt;

    public User(UUID id, EmailAddress email, String fullName, String passwordHash, Role role,
                Set<UUID> buildingIds, Instant createdAt) {
        this.id = Objects.requireNonNull(id, "User id is required");
        this.email = Objects.requireNonNull(email, "User email is required");
        this.fullName = requireFullName(fullName);
        this.passwordHash = requirePasswordHash(passwordHash);
        this.role = Objects.requireNonNull(role, "User role is required");
        this.buildingIds = buildingIds == null ? Set.of() : Set.copyOf(buildingIds);
        this.createdAt = Objects.requireNonNull(createdAt, "User creation time is required");
    }

    /**
     * Registers a new administrator (US01). It has no buildings until it registers or is linked to one.
     */
    public static User registerAdministrator(UUID id, EmailAddress email, String fullName, String passwordHash,
                                             Instant createdAt) {
        return new User(id, email, fullName, passwordHash, Role.ADMINISTRATOR, Set.of(), createdAt);
    }

    public UUID getId() {
        return id;
    }

    public EmailAddress getEmail() {
        return email;
    }

    public String getFullName() {
        return fullName;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public Role getRole() {
        return role;
    }

    public Set<UUID> getBuildingIds() {
        return buildingIds;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    @Override
    public String toString() {
        return "User[id=" + id + ", role=" + role + "]";
    }

    private static String requireFullName(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            throw new IllegalArgumentException("Full name is required");
        }
        String trimmed = fullName.trim();
        if (trimmed.length() > MAX_FULL_NAME_LENGTH) {
            throw new IllegalArgumentException(
                    "Full name must have at most " + MAX_FULL_NAME_LENGTH + " characters");
        }
        return trimmed;
    }

    private static String requirePasswordHash(String passwordHash) {
        if (passwordHash == null || passwordHash.isBlank()) {
            throw new IllegalArgumentException("Password hash is required");
        }
        return passwordHash;
    }
}
