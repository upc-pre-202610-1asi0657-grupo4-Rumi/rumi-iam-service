package com.rumi.iam.infrastructure.web.dto;

import com.rumi.iam.domain.model.DeviceToken;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(name = "DeviceTokenResponse", description = "A registered device, without its token")
public record DeviceTokenResponse(
        @Schema(description = "Id of the registered device", example = "5c1d7e9a-3b2f-4d6e-8a1c-9f0e2d4b6a8c")
        UUID id,
        @Schema(description = "Platform of the device", example = "ANDROID", allowableValues = {"ANDROID", "IOS", "WEB"})
        String platform,
        @Schema(description = "When the device was first registered", example = "2026-10-29T15:00:00Z")
        Instant registeredAt
) {

    public static DeviceTokenResponse from(DeviceToken deviceToken) {
        return new DeviceTokenResponse(deviceToken.getId(), deviceToken.getPlatform().name(),
                deviceToken.getRegisteredAt());
    }
}
