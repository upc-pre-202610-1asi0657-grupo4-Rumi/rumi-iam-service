package com.rumi.iam.application;

import com.rumi.iam.MutableClock;
import com.rumi.iam.UserFixtures;
import com.rumi.iam.domain.model.DevicePlatform;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DeviceTokenApplicationServiceTest {

    private static final UUID OTHER_USER_ID = UUID.fromString("e2f4a6c8-1b3d-4f5a-9c7e-0d2b4f6a8c1e");
    private static final String FIREBASE_TOKEN = "fcm-token-of-the-phone:APA91bH";

    private InMemoryDeviceTokenRepository repository;
    private MutableClock clock;
    private DeviceTokenApplicationService service;

    @BeforeEach
    void createService() {
        repository = new InMemoryDeviceTokenRepository();
        clock = new MutableClock(UserFixtures.CREATED_AT);
        service = new DeviceTokenApplicationService(repository, clock);
    }

    @Test
    void registersANewDevice() {
        RegisteredDeviceToken registered = service.register(UserFixtures.USER_ID, FIREBASE_TOKEN, DevicePlatform.ANDROID);

        assertThat(registered.created()).isTrue();
        assertThat(registered.deviceToken().getUserId()).isEqualTo(UserFixtures.USER_ID);
        assertThat(registered.deviceToken().getToken()).isEqualTo(FIREBASE_TOKEN);
        assertThat(registered.deviceToken().getPlatform()).isEqualTo(DevicePlatform.ANDROID);
        assertThat(registered.deviceToken().getRegisteredAt()).isEqualTo(UserFixtures.CREATED_AT);
        assertThat(repository.all()).containsExactly(registered.deviceToken());
    }

    @Test
    void registeringTheSameDeviceAgainDoesNotDuplicateItAndRefreshesIt() {
        RegisteredDeviceToken first = service.register(UserFixtures.USER_ID, FIREBASE_TOKEN, DevicePlatform.ANDROID);
        clock.advance(Duration.ofHours(1));

        RegisteredDeviceToken again = service.register(UserFixtures.USER_ID, FIREBASE_TOKEN, DevicePlatform.ANDROID);

        assertThat(again.created()).isFalse();
        assertThat(again.deviceToken().getId()).isEqualTo(first.deviceToken().getId());
        assertThat(again.deviceToken().getRegisteredAt()).isEqualTo(UserFixtures.CREATED_AT);
        assertThat(again.deviceToken().getUpdatedAt()).isEqualTo(clock.instant());
        assertThat(repository.all()).hasSize(1);
    }

    @Test
    void aDeviceRegisteredByAnotherUserMovesToTheCurrentUser() {
        RegisteredDeviceToken first = service.register(OTHER_USER_ID, FIREBASE_TOKEN, DevicePlatform.IOS);

        RegisteredDeviceToken moved = service.register(UserFixtures.USER_ID, FIREBASE_TOKEN, DevicePlatform.IOS);

        assertThat(moved.created()).isFalse();
        assertThat(moved.deviceToken().getId()).isEqualTo(first.deviceToken().getId());
        assertThat(moved.deviceToken().getUserId()).isEqualTo(UserFixtures.USER_ID);
        assertThat(repository.all()).singleElement()
                .satisfies(device -> assertThat(device.belongsTo(UserFixtures.USER_ID)).isTrue());
    }

    @Test
    void aUserCanHaveManyDevices() {
        service.register(UserFixtures.USER_ID, FIREBASE_TOKEN, DevicePlatform.ANDROID);
        service.register(UserFixtures.USER_ID, "fcm-token-of-the-tablet", DevicePlatform.WEB);

        assertThat(repository.all()).hasSize(2);
    }

    @Test
    void rejectsAnInvalidToken() {
        assertThatThrownBy(() -> service.register(UserFixtures.USER_ID, " ", DevicePlatform.ANDROID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Device token is required");
        assertThat(repository.all()).isEmpty();
    }
}
