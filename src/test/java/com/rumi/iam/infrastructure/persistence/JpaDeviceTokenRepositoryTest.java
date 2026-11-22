package com.rumi.iam.infrastructure.persistence;

import com.rumi.iam.PostgresTestContainer;
import com.rumi.iam.UserFixtures;
import com.rumi.iam.domain.model.DevicePlatform;
import com.rumi.iam.domain.model.DeviceToken;
import com.rumi.iam.domain.model.EmailAddress;
import com.rumi.iam.domain.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import({PostgresTestContainer.class, JpaUserRepository.class, JpaDeviceTokenRepository.class})
class JpaDeviceTokenRepositoryTest {

    private static final Instant NOW = UserFixtures.CREATED_AT;
    private static final String FIREBASE_TOKEN = "fcm-token-of-the-phone:APA91bH" + "x".repeat(150);

    @Autowired
    private JpaUserRepository users;

    @Autowired
    private JpaDeviceTokenRepository repository;

    private User otherUser;

    @BeforeEach
    void registerUsers() {
        users.save(UserFixtures.administrator());
        otherUser = users.save(User.registerAdministrator(UUID.randomUUID(), new EmailAddress("luis@rumi.pe"),
                "Luis Quispe", UserFixtures.PASSWORD_HASH, NOW));
    }

    @Test
    void savesAndFindsADeviceByItsToken() {
        DeviceToken device = repository.save(
                DeviceToken.register(UUID.randomUUID(), UserFixtures.USER_ID, FIREBASE_TOKEN, DevicePlatform.IOS, NOW));

        assertThat(repository.findByToken(FIREBASE_TOKEN)).hasValueSatisfying(found -> {
            assertThat(found.getId()).isEqualTo(device.getId());
            assertThat(found.getUserId()).isEqualTo(UserFixtures.USER_ID);
            assertThat(found.getToken()).isEqualTo(FIREBASE_TOKEN);
            assertThat(found.getPlatform()).isEqualTo(DevicePlatform.IOS);
            assertThat(found.getRegisteredAt()).isEqualTo(NOW);
            assertThat(found.getUpdatedAt()).isEqualTo(NOW);
        });
        assertThat(repository.findByToken("unknown")).isEmpty();
    }

    @Test
    void keepsAMaximumLengthToken() {
        String longest = "t".repeat(DeviceToken.MAX_TOKEN_LENGTH);

        repository.save(DeviceToken.register(UUID.randomUUID(), UserFixtures.USER_ID, longest, DevicePlatform.WEB, NOW));

        assertThat(repository.findByToken(longest)).isPresent();
    }

    @Test
    void updatesADeviceThatMovesToAnotherUser() {
        DeviceToken device = repository.save(
                DeviceToken.register(UUID.randomUUID(), UserFixtures.USER_ID, FIREBASE_TOKEN, DevicePlatform.ANDROID, NOW));

        repository.save(device.refreshFor(otherUser.getId(), DevicePlatform.ANDROID, NOW.plusSeconds(60)));

        assertThat(repository.findByToken(FIREBASE_TOKEN)).hasValueSatisfying(found -> {
            assertThat(found.getId()).isEqualTo(device.getId());
            assertThat(found.getUserId()).isEqualTo(otherUser.getId());
            assertThat(found.getRegisteredAt()).isEqualTo(NOW);
            assertThat(found.getUpdatedAt()).isEqualTo(NOW.plusSeconds(60));
        });
    }

    @Test
    void rejectsTheSameTokenTwice() {
        repository.save(
                DeviceToken.register(UUID.randomUUID(), UserFixtures.USER_ID, FIREBASE_TOKEN, DevicePlatform.ANDROID, NOW));

        assertThatThrownBy(() -> repository.save(
                DeviceToken.register(UUID.randomUUID(), otherUser.getId(), FIREBASE_TOKEN, DevicePlatform.ANDROID, NOW)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void rejectsADeviceOfAnUnknownUser() {
        assertThatThrownBy(() -> repository.save(
                DeviceToken.register(UUID.randomUUID(), UUID.randomUUID(), FIREBASE_TOKEN, DevicePlatform.ANDROID, NOW)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
