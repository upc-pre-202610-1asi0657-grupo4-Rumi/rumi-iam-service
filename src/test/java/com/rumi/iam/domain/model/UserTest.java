package com.rumi.iam.domain.model;

import com.rumi.iam.UserFixtures;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Disabled("Enabled by the next feat commit")
class UserTest {

    @Test
    void registersAnAdministratorWithoutBuildings() {
        User user = UserFixtures.administrator();

        assertThat(user.getId()).isEqualTo(UserFixtures.USER_ID);
        assertThat(user.getEmail()).isEqualTo(new EmailAddress(UserFixtures.EMAIL));
        assertThat(user.getFullName()).isEqualTo(UserFixtures.FULL_NAME);
        assertThat(user.getPasswordHash()).isEqualTo(UserFixtures.PASSWORD_HASH);
        assertThat(user.getRole()).isEqualTo(Role.ADMINISTRATOR);
        assertThat(user.getBuildingIds()).isEmpty();
        assertThat(user.getCreatedAt()).isEqualTo(UserFixtures.CREATED_AT);
    }

    @Test
    void trimsTheFullName() {
        User user = User.registerAdministrator(UserFixtures.USER_ID, new EmailAddress(UserFixtures.EMAIL),
                "  Ana Torres  ", UserFixtures.PASSWORD_HASH, UserFixtures.CREATED_AT);

        assertThat(user.getFullName()).isEqualTo("Ana Torres");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void rejectsAMissingFullName(String fullName) {
        assertThatThrownBy(() -> User.registerAdministrator(UserFixtures.USER_ID, new EmailAddress(UserFixtures.EMAIL),
                fullName, UserFixtures.PASSWORD_HASH, UserFixtures.CREATED_AT))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Full name is required");
    }

    @Test
    void rejectsAFullNameLongerThanTheMaximum() {
        assertThatThrownBy(() -> User.registerAdministrator(UserFixtures.USER_ID, new EmailAddress(UserFixtures.EMAIL),
                "a".repeat(User.MAX_FULL_NAME_LENGTH + 1), UserFixtures.PASSWORD_HASH, UserFixtures.CREATED_AT))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Full name must have at most 100 characters");
    }

    @ParameterizedTest
    @NullAndEmptySource
    void rejectsAMissingPasswordHash(String passwordHash) {
        assertThatThrownBy(() -> User.registerAdministrator(UserFixtures.USER_ID, new EmailAddress(UserFixtures.EMAIL),
                UserFixtures.FULL_NAME, passwordHash, UserFixtures.CREATED_AT))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Password hash is required");
    }

    @Test
    void requiresAnIdAnEmailARoleAndACreationTime() {
        EmailAddress email = new EmailAddress(UserFixtures.EMAIL);
        String hash = UserFixtures.PASSWORD_HASH;
        Instant createdAt = UserFixtures.CREATED_AT;

        assertThatThrownBy(() -> new User(null, email, "Ana", hash, Role.ADMINISTRATOR, Set.of(), createdAt))
                .isInstanceOf(NullPointerException.class).hasMessage("User id is required");
        assertThatThrownBy(() -> new User(UUID.randomUUID(), null, "Ana", hash, Role.ADMINISTRATOR, Set.of(), createdAt))
                .isInstanceOf(NullPointerException.class).hasMessage("User email is required");
        assertThatThrownBy(() -> new User(UUID.randomUUID(), email, "Ana", hash, null, Set.of(), createdAt))
                .isInstanceOf(NullPointerException.class).hasMessage("User role is required");
        assertThatThrownBy(() -> new User(UUID.randomUUID(), email, "Ana", hash, Role.ADMINISTRATOR, Set.of(), null))
                .isInstanceOf(NullPointerException.class).hasMessage("User creation time is required");
    }

    @Test
    void keepsAnUnmodifiableCopyOfTheBuildingIds() {
        Set<UUID> buildingIds = new HashSet<>(Set.of(UUID.randomUUID()));
        User user = new User(UserFixtures.USER_ID, new EmailAddress(UserFixtures.EMAIL), UserFixtures.FULL_NAME,
                UserFixtures.PASSWORD_HASH, Role.RESIDENT, buildingIds, UserFixtures.CREATED_AT);

        buildingIds.add(UUID.randomUUID());

        assertThat(user.getBuildingIds()).hasSize(1);
        assertThatThrownBy(() -> user.getBuildingIds().add(UUID.randomUUID()))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void treatsMissingBuildingIdsAsNone() {
        User user = new User(UserFixtures.USER_ID, new EmailAddress(UserFixtures.EMAIL), UserFixtures.FULL_NAME,
                UserFixtures.PASSWORD_HASH, Role.ADMINISTRATOR, null, UserFixtures.CREATED_AT);

        assertThat(user.getBuildingIds()).isEmpty();
    }

    @Test
    void neverPrintsThePasswordHash() {
        assertThat(UserFixtures.administrator().toString()).doesNotContain(UserFixtures.PASSWORD_HASH);
    }
}
