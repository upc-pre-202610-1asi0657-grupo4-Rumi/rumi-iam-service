package com.rumi.iam.application;

import com.rumi.iam.domain.model.DeviceToken;
import com.rumi.iam.domain.repository.DeviceTokenRepository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

class InMemoryDeviceTokenRepository implements DeviceTokenRepository {

    private final Map<UUID, DeviceToken> devices = new LinkedHashMap<>();

    @Override
    public Optional<DeviceToken> findByToken(String token) {
        return devices.values().stream().filter(device -> device.getToken().equals(token)).findFirst();
    }

    @Override
    public DeviceToken save(DeviceToken deviceToken) {
        devices.put(deviceToken.getId(), deviceToken);
        return deviceToken;
    }

    List<DeviceToken> all() {
        return List.copyOf(devices.values());
    }
}
