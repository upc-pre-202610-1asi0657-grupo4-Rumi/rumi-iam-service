package com.rumi.iam.infrastructure.persistence;

import com.rumi.iam.domain.model.EmailAddress;
import com.rumi.iam.domain.model.User;
import com.rumi.iam.domain.repository.UserRepository;

import java.util.Optional;
import java.util.UUID;

public class JpaUserRepository implements UserRepository {

    @Override
    public Optional<User> findById(UUID id) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public Optional<User> findByEmail(EmailAddress email) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public boolean existsByEmail(EmailAddress email) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public User save(User user) {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
