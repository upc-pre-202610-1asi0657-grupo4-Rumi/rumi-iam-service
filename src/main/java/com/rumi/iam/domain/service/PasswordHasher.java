package com.rumi.iam.domain.service;

/**
 * One-way hashing of plain-text passwords. Implementations must never log or keep the plain text.
 */
public interface PasswordHasher {

    String hash(String rawPassword);

    boolean matches(String rawPassword, String passwordHash);
}
