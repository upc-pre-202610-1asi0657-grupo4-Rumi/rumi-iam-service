package com.rumi.iam.infrastructure.web.dto;

import com.rumi.iam.infrastructure.web.validation.PasswordLength;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(name = "ResetPasswordRequest", description = "Token of the reset link and the new password")
public record ResetPasswordRequest(
        @Schema(description = "Value of the 'token' parameter of the reset link",
                example = "q3J9Zf1x7Lw0hX2bV8nR4tY6uK5mP1sD3gA7cE9iO0w")
        @NotBlank @Size(max = MAX_TOKEN_LENGTH)
        String token,
        @Schema(description = "At least 8 characters and at most 72 bytes in UTF-8", example = "Nueva-Clave-Sismo-2026",
                format = "password")
        @NotNull @PasswordLength
        String newPassword
) {

    public static final int MAX_TOKEN_LENGTH = 128;

    /**
     * Never prints the token or the password, so that they cannot leak into logs.
     */
    @Override
    public String toString() {
        return "ResetPasswordRequest[]";
    }
}
