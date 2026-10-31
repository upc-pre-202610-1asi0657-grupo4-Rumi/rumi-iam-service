package com.rumi.iam;

import com.rumi.iam.domain.model.EmailAddress;
import com.rumi.iam.domain.model.User;

import java.time.Instant;
import java.util.UUID;

public final class UserFixtures {

    public static final UUID USER_ID = UUID.fromString("7d3c5b1e-2f4a-4c8e-9b6d-1a2b3c4d5e6f");
    public static final String EMAIL = "ana.torres@rumi.pe";
    public static final String FULL_NAME = "Ana Torres";
    public static final String PASSWORD = "Sismo-Seguro-2026";
    // A BCrypt hash shape; its plain text is irrelevant to the tests that use it.
    public static final String PASSWORD_HASH = "$2a$10$abcdefghijklmnopqrstuuJ0kzq5u3cYpZ8iR5x2Qx2b4c6d8e0fG";
    public static final Instant CREATED_AT = Instant.parse("2026-10-29T15:00:00Z");

    private UserFixtures() {
    }

    public static User administrator() {
        return User.registerAdministrator(USER_ID, new EmailAddress(EMAIL), FULL_NAME, PASSWORD_HASH, CREATED_AT);
    }
}
