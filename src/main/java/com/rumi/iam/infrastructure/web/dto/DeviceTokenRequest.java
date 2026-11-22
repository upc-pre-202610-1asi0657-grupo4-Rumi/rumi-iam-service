package com.rumi.iam.infrastructure.web.dto;

import com.rumi.iam.domain.model.DevicePlatform;
import com.rumi.iam.domain.model.DeviceToken;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(name = "DeviceTokenRequest", description = "Firebase token of the device of the signed-in user")
public record DeviceTokenRequest(
        @Schema(description = "Firebase Cloud Messaging registration token of the device",
                example = "dQw4w9WgXcQ:APA91bHun4MxP5egoKMwt2KZFBaFUH-1RYqx")
        @NotBlank @Size(max = DeviceToken.MAX_TOKEN_LENGTH)
        String token,
        @Schema(description = "Platform of the device", example = "ANDROID", allowableValues = {"ANDROID", "IOS", "WEB"})
        @NotNull @Pattern(regexp = "ANDROID|IOS|WEB", message = "must be one of ANDROID, IOS, WEB")
        String platform
) {

    public DevicePlatform devicePlatform() {
        return DevicePlatform.valueOf(platform);
    }

    /**
     * Never prints the token, so that it cannot leak into logs.
     */
    @Override
    public String toString() {
        return "DeviceTokenRequest[platform=" + platform + "]";
    }
}
