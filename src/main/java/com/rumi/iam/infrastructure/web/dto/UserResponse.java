package com.rumi.iam.infrastructure.web.dto;

import com.rumi.iam.domain.model.User;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(name = "UserResponse", description = "A registered user, without any password data")
public record UserResponse(
        @Schema(description = "Id of the user", example = "7d3c5b1e-2f4a-4c8e-9b6d-1a2b3c4d5e6f")
        UUID id,
        @Schema(description = "Email of the user, in lowercase", example = "ana.torres@rumi.pe")
        String email,
        @Schema(description = "Full name of the user", example = "Ana Torres")
        String fullName,
        @Schema(description = "Role of the user", example = "ADMINISTRATOR", allowableValues = {"ADMINISTRATOR", "RESIDENT"})
        String role
) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getEmail().value(), user.getFullName(), user.getRole().name());
    }
}
