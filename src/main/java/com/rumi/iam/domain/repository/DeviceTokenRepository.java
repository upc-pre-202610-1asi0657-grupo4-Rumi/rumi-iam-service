package com.rumi.iam.domain.repository;

import com.rumi.iam.domain.model.DeviceToken;

import java.util.Optional;

public interface DeviceTokenRepository {

    Optional<DeviceToken> findByToken(String token);

    DeviceToken save(DeviceToken deviceToken);
}
