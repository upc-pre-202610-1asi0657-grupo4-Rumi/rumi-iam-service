package com.rumi.iam.domain.repository;

import com.rumi.iam.domain.model.EmailAddress;
import com.rumi.iam.domain.model.User;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository {

    Optional<User> findById(UUID id);

    Optional<User> findByEmail(EmailAddress email);

    boolean existsByEmail(EmailAddress email);

    User save(User user);
}
