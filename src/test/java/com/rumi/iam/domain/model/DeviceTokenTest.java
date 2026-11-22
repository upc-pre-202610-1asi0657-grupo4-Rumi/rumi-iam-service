package com.rumi.iam.domain.model;

import com.rumi.iam.UserFixtures;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DeviceTokenTest {

    private static final UUID DEVICE_ID = UUID.fromString("5c1d7e9a-3b2f-4d6e-8a1c-9f0e2d4b6a8c");
    private static final UUID OTHER_USER_ID = UUID.fromString("e2f4a6c8-1b3d-4f5a-9c7e-0d2b4f6a8c1e");
    private static final String FIREBASE_TOKEN = "fcm-token-of-the-phone:APA91bH";
    private static final Instant NOW = UserFixtures.CREATED_AT;

    private static DeviceToken registered() {
        return DeviceToken.register(DEVICE_ID, UserFixtures.USER_ID, FIREBASE_TOKEN, DevicePlatform.ANDROID, NOW);
    }

    @Test
    void isRegisteredForAUser() {
        DeviceToken device = registered();

        assertThat(device.getId()).isEqualTo(DEVICE_ID);
        assertThat(device.getUserId()).isEqualTo(UserFixtures.USER_ID);
        assertThat(device.getToken()).isEqualTo(FIREBASE_TOKEN);
        assertThat(device.getPlatform()).isEqualTo(DevicePlatform.ANDROID);
        assertThat(device.getRegisteredAt()).isEqualTo(NOW);
        assertThat(device.getUpdatedAt()).isEqualTo(NOW);
    }

    @Test
    void refreshingForTheSameUserKeepsItsIdAndRegistrationTime() {
        DeviceToken refreshed = registered().refreshFor(UserFixtures.USER_ID, DevicePlatform.ANDROID, NOW.plusSeconds(60));

        assertThat(refreshed.getId()).isEqualTo(DEVICE_ID);
        assertThat(refreshed.getUserId()).isEqualTo(UserFixtures.USER_ID);
        assertThat(refreshed.getRegisteredAt()).isEqualTo(NOW);
        assertThat(refreshed.getUpdatedAt()).isEqualTo(NOW.plusSeconds(60));
    }

    @Test
    void refreshingForAnotherUserMovesTheDevice() {
        DeviceToken moved = registered().refreshFor(OTHER_USER_ID, DevicePlatform.IOS, NOW.plusSeconds(60));

        assertThat(moved.getId()).isEqualTo(DEVICE_ID);
        assertThat(moved.getUserId()).isEqualTo(OTHER_USER_ID);
        assertThat(moved.getPlatform()).isEqualTo(DevicePlatform.IOS);
        assertThat(moved.getToken()).isEqualTo(FIREBASE_TOKEN);
    }

    @Test
    void tellsWhetherItBelongsToAUser() {
        assertThat(registered().belongsTo(UserFixtures.USER_ID)).isTrue();
        assertThat(registered().belongsTo(OTHER_USER_ID)).isFalse();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void rejectsAMissingToken(String token) {
        assertThatThrownBy(() -> DeviceToken.register(DEVICE_ID, UserFixtures.USER_ID, token, DevicePlatform.WEB, NOW))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Device token is required");
    }

    @Test
    void rejectsATokenLongerThanTheMaximum() {
        String tooLong = "t".repeat(DeviceToken.MAX_TOKEN_LENGTH + 1);

        assertThatThrownBy(() -> DeviceToken.register(DEVICE_ID, UserFixtures.USER_ID, tooLong, DevicePlatform.WEB, NOW))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Device token must have at most 1024 characters");
    }

    @Test
    void rejectsAMissingPlatform() {
        assertThatThrownBy(() -> DeviceToken.register(DEVICE_ID, UserFixtures.USER_ID, FIREBASE_TOKEN, null, NOW))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("Device platform is required");
    }

    @Test
    void doesNotPrintTheToken() {
        assertThat(registered().toString()).doesNotContain(FIREBASE_TOKEN);
    }
}
