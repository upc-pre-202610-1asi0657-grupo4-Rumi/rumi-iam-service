package com.rumi.iam.domain.model;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public final class User {

    public static final int MAX_FULL_NAME_LENGTH = 100;

    public User(UUID id, EmailAddress email, String fullName, String passwordHash, Role role,
                Set<UUID> buildingIds, Instant createdAt) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public static User registerAdministrator(UUID id, EmailAddress email, String fullName, String passwordHash,
                                             Instant createdAt) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public UUID getId() {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public EmailAddress getEmail() {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public String getFullName() {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public String getPasswordHash() {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public Role getRole() {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public Set<UUID> getBuildingIds() {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public Instant getCreatedAt() {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
