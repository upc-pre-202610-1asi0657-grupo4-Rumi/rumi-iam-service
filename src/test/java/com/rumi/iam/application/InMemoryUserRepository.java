package com.rumi.iam.application;

import com.rumi.iam.domain.model.EmailAddress;
import com.rumi.iam.domain.model.User;
import com.rumi.iam.domain.repository.UserRepository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

class InMemoryUserRepository implements UserRepository {

    private final Map<UUID, User> users = new LinkedHashMap<>();

    @Override
    public Optional<User> findById(UUID id) {
        return Optional.ofNullable(users.get(id));
    }

    @Override
    public Optional<User> findByEmail(EmailAddress email) {
        return users.values().stream().filter(user -> user.getEmail().equals(email)).findFirst();
    }

    @Override
    public boolean existsByEmail(EmailAddress email) {
        return findByEmail(email).isPresent();
    }

    @Override
    public User save(User user) {
        users.put(user.getId(), user);
        return user;
    }

    List<User> all() {
        return List.copyOf(users.values());
    }
}
