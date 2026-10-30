package com.rumi.iam.infrastructure.web.dto;

import com.rumi.iam.application.AuthenticatedUser;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(name = "AuthenticationResponse", description = "The signed-in user and its access token")
public record AuthenticationResponse(
        @Schema(description = "Id of the user", example = "7d3c5b1e-2f4a-4c8e-9b6d-1a2b3c4d5e6f")
        UUID userId,
        @Schema(description = "Email of the user, in lowercase", example = "ana.torres@rumi.pe")
        String email,
        @Schema(description = "Role of the user", example = "ADMINISTRATOR", allowableValues = {"ADMINISTRATOR", "RESIDENT"})
        String role,
        @Schema(description = "HS256 JWT to send as 'Authorization: Bearer <token>' through the API gateway")
        String token,
        @Schema(description = "Type of the token", example = "Bearer")
        String tokenType,
        @Schema(description = "When the token expires", example = "2026-10-29T16:00:00Z")
        Instant expiresAt
) {

    public static final String BEARER = "Bearer";

    public static AuthenticationResponse from(AuthenticatedUser authenticated) {
        return new AuthenticationResponse(
                authenticated.user().getId(),
                authenticated.user().getEmail().value(),
                authenticated.user().getRole().name(),
                authenticated.token().value(),
                BEARER,
                authenticated.token().expiresAt()
        );
    }

    /**
     * Never prints the token, so that it cannot leak into logs.
     */
    @Override
    public String toString() {
        return "AuthenticationResponse[userId=" + userId + ", role=" + role + ", expiresAt=" + expiresAt + "]";
    }
}
