package com.rumi.iam.infrastructure.persistence;

import com.rumi.iam.PostgresTestContainer;
import com.rumi.iam.UserFixtures;
import com.rumi.iam.domain.model.EmailAddress;
import com.rumi.iam.domain.model.PasswordResetToken;
import com.rumi.iam.domain.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import({PostgresTestContainer.class, JpaUserRepository.class, JpaPasswordResetTokenRepository.class})
class JpaPasswordResetTokenRepositoryTest {

    private static final Instant NOW = UserFixtures.CREATED_AT;
    private static final Duration TTL = Duration.ofMinutes(30);

    @Autowired
    private JpaUserRepository users;

    @Autowired
    private JpaPasswordResetTokenRepository repository;

    private User otherUser;

    @BeforeEach
    void registerUsers() {
        users.save(UserFixtures.administrator());
        otherUser = users.save(User.registerAdministrator(UUID.randomUUID(), new EmailAddress("luis@rumi.pe"),
                "Luis Quispe", UserFixtures.PASSWORD_HASH, NOW));
    }

    private static PasswordResetToken tokenFor(UUID userId, char hashCharacter) {
        return PasswordResetToken.issue(UUID.randomUUID(), userId, String.valueOf(hashCharacter).repeat(64), NOW, TTL);
    }

    @Test
    void savesAndFindsATokenByItsHash() {
        PasswordResetToken token = repository.save(tokenFor(UserFixtures.USER_ID, 'a'));

        assertThat(repository.findByTokenHash("a".repeat(64))).hasValueSatisfying(found -> {
            assertThat(found.getId()).isEqualTo(token.getId());
            assertThat(found.getUserId()).isEqualTo(UserFixtures.USER_ID);
            assertThat(found.getExpiresAt()).isEqualTo(NOW.plus(TTL));
            assertThat(found.getCreatedAt()).isEqualTo(NOW);
            assertThat(found.getUsedAt()).isNull();
        });
        assertThat(repository.findByTokenHash("b".repeat(64))).isEmpty();
    }

    @Test
    void keepsTheTimeATokenWasUsed() {
        PasswordResetToken token = repository.save(tokenFor(UserFixtures.USER_ID, 'a'));

        repository.save(token.markUsed(NOW.plusSeconds(90)));

        assertThat(repository.findByTokenHash("a".repeat(64)))
                .hasValueSatisfying(found -> assertThat(found.getUsedAt()).isEqualTo(NOW.plusSeconds(90)));
    }

    @Test
    void rejectsTwoTokensWithTheSameHash() {
        repository.save(tokenFor(UserFixtures.USER_ID, 'a'));

        assertThatThrownBy(() -> repository.save(tokenFor(otherUser.getId(), 'a')))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void rejectsATokenOfAnUnknownUser() {
        assertThatThrownBy(() -> repository.save(tokenFor(UUID.randomUUID(), 'a')))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void invalidatesOnlyTheUnusedTokensOfTheGivenUser() {
        repository.save(tokenFor(UserFixtures.USER_ID, 'a'));
        repository.save(tokenFor(UserFixtures.USER_ID, 'b'));
        repository.save(tokenFor(UserFixtures.USER_ID, 'c').markUsed(NOW.plusSeconds(10)));
        repository.save(tokenFor(otherUser.getId(), 'd'));

        repository.invalidateUnusedByUserId(UserFixtures.USER_ID, NOW.plusSeconds(60));

        assertThat(repository.findByTokenHash("a".repeat(64)))
                .hasValueSatisfying(found -> assertThat(found.getUsedAt()).isEqualTo(NOW.plusSeconds(60)));
        assertThat(repository.findByTokenHash("b".repeat(64)))
                .hasValueSatisfying(found -> assertThat(found.getUsedAt()).isEqualTo(NOW.plusSeconds(60)));
        assertThat(repository.findByTokenHash("c".repeat(64)))
                .hasValueSatisfying(found -> assertThat(found.getUsedAt()).isEqualTo(NOW.plusSeconds(10)));
        assertThat(repository.findByTokenHash("d".repeat(64)))
                .hasValueSatisfying(found -> assertThat(found.getUsedAt()).isNull());
    }
}
