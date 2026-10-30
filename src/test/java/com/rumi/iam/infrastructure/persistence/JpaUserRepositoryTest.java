package com.rumi.iam.infrastructure.persistence;

import com.rumi.iam.UserFixtures;
import com.rumi.iam.application.EmailAlreadyRegisteredException;
import com.rumi.iam.domain.model.EmailAddress;
import com.rumi.iam.domain.model.Role;
import com.rumi.iam.domain.model.User;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Disabled("Enabled by the next feat commit")
@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@ActiveProfiles("test")
@Import(JpaUserRepository.class)
class JpaUserRepositoryTest {

    @Autowired
    private JpaUserRepository repository;

    @Test
    void savesAndFindsAUserById() {
        repository.save(UserFixtures.administrator());

        assertThat(repository.findById(UserFixtures.USER_ID)).hasValueSatisfying(found -> {
            assertThat(found.getEmail()).isEqualTo(new EmailAddress(UserFixtures.EMAIL));
            assertThat(found.getFullName()).isEqualTo(UserFixtures.FULL_NAME);
            assertThat(found.getPasswordHash()).isEqualTo(UserFixtures.PASSWORD_HASH);
            assertThat(found.getRole()).isEqualTo(Role.ADMINISTRATOR);
            assertThat(found.getBuildingIds()).isEmpty();
            assertThat(found.getCreatedAt()).isEqualTo(UserFixtures.CREATED_AT);
        });
    }

    @Test
    void findsAUserByEmail() {
        repository.save(UserFixtures.administrator());

        assertThat(repository.findByEmail(new EmailAddress("ANA.TORRES@rumi.pe")))
                .hasValueSatisfying(found -> assertThat(found.getId()).isEqualTo(UserFixtures.USER_ID));
        assertThat(repository.existsByEmail(new EmailAddress(UserFixtures.EMAIL))).isTrue();
    }

    @Test
    void findsNothingForAnUnknownIdOrEmail() {
        assertThat(repository.findById(UUID.randomUUID())).isEmpty();
        assertThat(repository.findByEmail(new EmailAddress("nobody@rumi.pe"))).isEmpty();
        assertThat(repository.existsByEmail(new EmailAddress("nobody@rumi.pe"))).isFalse();
    }

    @Test
    void keepsTheBuildingIdsOfAUser() {
        Set<UUID> buildingIds = Set.of(
                UUID.fromString("3f2c8a10-6b7d-4e9f-a1b2-c3d4e5f60718"),
                UUID.fromString("9a8b7c6d-5e4f-4a3b-8c2d-1e0f9a8b7c6d"));
        User resident = new User(UUID.randomUUID(), new EmailAddress("luis@rumi.pe"), "Luis Quispe",
                UserFixtures.PASSWORD_HASH, Role.RESIDENT, buildingIds, UserFixtures.CREATED_AT);

        repository.save(resident);

        assertThat(repository.findById(resident.getId())).hasValueSatisfying(found -> {
            assertThat(found.getRole()).isEqualTo(Role.RESIDENT);
            assertThat(found.getBuildingIds()).containsExactlyInAnyOrderElementsOf(buildingIds);
        });
    }

    @Test
    void rejectsASecondUserWithTheSameEmail() {
        repository.save(UserFixtures.administrator());
        User sameEmail = User.registerAdministrator(UUID.randomUUID(), new EmailAddress(UserFixtures.EMAIL),
                "Someone Else", UserFixtures.PASSWORD_HASH, UserFixtures.CREATED_AT);

        assertThatThrownBy(() -> repository.save(sameEmail)).isInstanceOf(EmailAlreadyRegisteredException.class);
    }
}
