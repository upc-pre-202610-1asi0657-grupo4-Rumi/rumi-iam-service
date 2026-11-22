package com.rumi.iam.application;

import com.rumi.iam.domain.model.DevicePlatform;
import com.rumi.iam.domain.model.DeviceToken;
import com.rumi.iam.domain.repository.DeviceTokenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

/**
 * Registers the Firebase token of a device of the signed-in user, so that Notification can push to it (US14).
 */
@Service
public class DeviceTokenApplicationService {

    private final DeviceTokenRepository deviceTokenRepository;
    private final Clock clock;

    public DeviceTokenApplicationService(DeviceTokenRepository deviceTokenRepository, Clock clock) {
        this.deviceTokenRepository = deviceTokenRepository;
        this.clock = clock;
    }

    /**
     * Stores a new device, or refreshes a known one. A token already registered by another user (the phone
     * changed account) moves to {@code userId}.
     */
    @Transactional
    public RegisteredDeviceToken register(UUID userId, String token, DevicePlatform platform) {
        Instant now = clock.instant();
        return deviceTokenRepository.findByToken(token)
                .map(known -> new RegisteredDeviceToken(
                        deviceTokenRepository.save(known.refreshFor(userId, platform, now)), false))
                .orElseGet(() -> new RegisteredDeviceToken(
                        deviceTokenRepository.save(DeviceToken.register(UUID.randomUUID(), userId, token, platform, now)),
                        true));
    }
}
