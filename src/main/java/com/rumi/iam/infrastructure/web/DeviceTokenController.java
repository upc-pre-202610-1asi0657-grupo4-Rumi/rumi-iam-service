package com.rumi.iam.infrastructure.web;

import com.rumi.iam.application.DeviceTokenApplicationService;
import com.rumi.iam.application.RegisteredDeviceToken;
import com.rumi.iam.infrastructure.web.dto.DeviceTokenRequest;
import com.rumi.iam.infrastructure.web.dto.DeviceTokenResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Devices of the signed-in user. The API gateway requires a valid token on {@code /api/v1/device-tokens/**} and
 * passes the user id in {@code X-User-Id}.
 */
@RestController
@RequestMapping("/api/v1/device-tokens")
@Tag(name = "Device tokens", description = "Firebase tokens of the devices that receive earthquake alerts (US14)")
public class DeviceTokenController {

    private final DeviceTokenApplicationService deviceTokenService;

    public DeviceTokenController(DeviceTokenApplicationService deviceTokenService) {
        this.deviceTokenService = deviceTokenService;
    }

    @Operation(
            summary = "Register the device of the signed-in user",
            description = "US14. Stores the Firebase token of the device so that Notification can push earthquake "
                    + "alerts to it. Registering a known token again refreshes it; a token registered by another "
                    + "user moves to the signed-in user. Call it through the API gateway with "
                    + "'Authorization: Bearer <token>'; the gateway sets X-User-Id."
    )
    @Parameter(name = CurrentUserId.HEADER, in = ParameterIn.HEADER, required = true,
            description = "Id of the signed-in user, set by the API gateway from the token; clients do not send it",
            example = "7d3c5b1e-2f4a-4c8e-9b6d-1a2b3c4d5e6f")
    @ApiResponse(responseCode = "201", description = "The device was registered",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = DeviceTokenResponse.class)))
    @ApiResponse(responseCode = "200", description = "The device was already registered; it was refreshed",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = DeviceTokenResponse.class)))
    @ApiResponse(responseCode = "400", description = "Invalid data; field errors are listed in 'errors'",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class),
                    examples = @ExampleObject(name = "Unknown platform", value = """
                            {"type":"about:blank","title":"Bad Request","status":400,\
                            "detail":"Invalid request content.","instance":"/api/v1/device-tokens",\
                            "errors":{"platform":"must be one of ANDROID, IOS, WEB"}}""")))
    @ApiResponse(responseCode = "401", description = "The request did not come through the gateway with a valid token",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class),
                    examples = @ExampleObject(name = "Missing X-User-Id", value = """
                            {"type":"about:blank","title":"Unauthorized","status":401,\
                            "detail":"The request has no valid X-User-Id header. Call this endpoint through the API \
                            gateway with a valid token.","instance":"/api/v1/device-tokens"}""")))
    @PostMapping
    public ResponseEntity<DeviceTokenResponse> register(
            @Parameter(hidden = true) @CurrentUserId UUID userId,
            @Valid @RequestBody DeviceTokenRequest request
    ) {
        RegisteredDeviceToken registered = deviceTokenService.register(userId, request.token(), request.devicePlatform());
        HttpStatus status = registered.created() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(DeviceTokenResponse.from(registered.deviceToken()));
    }
}
