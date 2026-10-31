package com.rumi.iam.infrastructure.security;

import com.rumi.iam.UserFixtures;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BCryptPasswordHasherTest {

    private final BCryptPasswordHasher hasher = new BCryptPasswordHasher();

    @Test
    void producesABCryptHashThatIsNotThePassword() {
        String hash = hasher.hash(UserFixtures.PASSWORD);

        assertThat(hash).startsWith("$2").hasSize(60).doesNotContain(UserFixtures.PASSWORD);
    }

    @Test
    void saltsEveryHash() {
        assertThat(hasher.hash(UserFixtures.PASSWORD)).isNotEqualTo(hasher.hash(UserFixtures.PASSWORD));
    }

    @Test
    void matchesOnlyTheRightPassword() {
        String hash = hasher.hash(UserFixtures.PASSWORD);

        assertThat(hasher.matches(UserFixtures.PASSWORD, hash)).isTrue();
        assertThat(hasher.matches("wrong-password", hash)).isFalse();
    }

    @Test
    void matchesAMultibytePassword() {
        String password = "contraseña-sísmica";
        String hash = hasher.hash(password);

        assertThat(hasher.matches(password, hash)).isTrue();
        assertThat(hasher.matches("contrasena-sismica", hash)).isFalse();
    }
}
