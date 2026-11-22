package com.rumi.iam.application;

import com.rumi.iam.domain.model.DeviceToken;

/**
 * Result of registering a device: the stored device and whether it is new ({@code created}) or was already known.
 */
public record RegisteredDeviceToken(DeviceToken deviceToken, boolean created) {
}
