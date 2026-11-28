package com.rumi.iam.infrastructure.seed;

import com.rumi.iam.domain.model.EmailAddress;
import com.rumi.iam.domain.model.Role;
import com.rumi.iam.domain.model.User;
import com.rumi.iam.domain.repository.UserRepository;
import com.rumi.iam.domain.service.PasswordHasher;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Set;
import java.util.UUID;

/**
 * Demo users of the dev profile, already linked to the sample building that the other services seed. The
 * password is a public demo value for local use only; this class never runs outside the dev profile.
 */
@Component
@Profile("dev")
public class DevDataSeeder implements ApplicationRunner {

    public static final UUID BUILDING_ID = UUID.fromString("7a9b3c1d-2e4f-4b6a-8c0d-1e2f3a4b5c6d");
    public static final UUID ADMINISTRATOR_ID = UUID.fromString("c1d2e3f4-a5b6-4c7d-8e9f-0a1b2c3d4e5f");
    public static final UUID RESIDENT_ID = UUID.fromString("d4e5f6a7-b8c9-4d0e-9f1a-2b3c4d5e6f70");
    public static final String ADMINISTRATOR_EMAIL = "admin.demo@rumi.dev";
    public static final String RESIDENT_EMAIL = "residente.demo@rumi.dev";
    public static final String DEMO_PASSWORD = "rumi-demo-2026";

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final Clock clock;

    public DevDataSeeder(UserRepository userRepository, PasswordHasher passwordHasher, Clock clock) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seed(ADMINISTRATOR_ID, ADMINISTRATOR_EMAIL, "Administradora Demo", Role.ADMINISTRATOR);
        seed(RESIDENT_ID, RESIDENT_EMAIL, "Residente Demo", Role.RESIDENT);
    }

    private void seed(UUID id, String email, String fullName, Role role) {
        EmailAddress address = new EmailAddress(email);
        if (userRepository.existsByEmail(address)) {
            return;
        }
        userRepository.save(new User(id, address, fullName, passwordHasher.hash(DEMO_PASSWORD), role,
                Set.of(BUILDING_ID), clock.instant()));
    }
}
