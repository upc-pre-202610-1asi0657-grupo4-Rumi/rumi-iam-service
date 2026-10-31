package com.rumi.iam.infrastructure.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(name = "SignInRequest", description = "Credentials of a registered user")
public record SignInRequest(
        @Schema(description = "Email of the user, in any case", example = "ana.torres@rumi.pe")
        @NotBlank
        String email,
        @Schema(description = "Password of the user", example = "Sismo-Seguro-2026", format = "password")
        @NotBlank
        String password
) {

    /**
     * Never prints the password, so that it cannot leak into logs.
     */
    @Override
    public String toString() {
        return "SignInRequest[email=" + email + "]";
    }
}
