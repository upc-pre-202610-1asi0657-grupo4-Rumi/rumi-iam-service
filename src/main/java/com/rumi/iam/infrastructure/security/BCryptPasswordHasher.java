package com.rumi.iam.infrastructure.security;

import com.rumi.iam.domain.service.PasswordHasher;

public class BCryptPasswordHasher implements PasswordHasher {

    @Override
    public String hash(String rawPassword) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public boolean matches(String rawPassword, String passwordHash) {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
