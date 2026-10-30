package com.rumi.iam.application;

import com.rumi.iam.domain.service.PasswordHasher;

import java.util.ArrayList;
import java.util.List;

/**
 * Reversible on purpose so that tests can see what was hashed. It also records every hash it compared against.
 */
class FakePasswordHasher implements PasswordHasher {

    static final String PREFIX = "hashed:";

    final List<String> comparedHashes = new ArrayList<>();
    int hashCalls;

    @Override
    public String hash(String rawPassword) {
        hashCalls++;
        return PREFIX + rawPassword;
    }

    @Override
    public boolean matches(String rawPassword, String passwordHash) {
        comparedHashes.add(passwordHash);
        return (PREFIX + rawPassword).equals(passwordHash);
    }
}
