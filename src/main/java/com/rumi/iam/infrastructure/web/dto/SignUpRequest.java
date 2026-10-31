package com.rumi.iam.infrastructure.web.dto;

import com.rumi.iam.domain.model.EmailAddress;
import com.rumi.iam.domain.model.User;
import com.rumi.iam.infrastructure.web.validation.PasswordLength;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(name = "SignUpRequest", description = "Data of the administrator to register")
public record SignUpRequest(
        @Schema(description = "Email used to sign in; stored in lowercase", example = "ana.torres@rumi.pe")
        @NotBlank @Email @Size(max = EmailAddress.MAX_LENGTH)
        String email,
        @Schema(description = "At least 8 characters and at most 72 bytes in UTF-8", example = "Sismo-Seguro-2026",
                format = "password")
        @NotNull @PasswordLength
        String password,
        @Schema(description = "Full name of the administrator", example = "Ana Torres")
        @NotBlank @Size(max = User.MAX_FULL_NAME_LENGTH)
        String fullName
) {

    /**
     * Never prints the password, so that it cannot leak into logs.
     */
    @Override
    public String toString() {
        return "SignUpRequest[email=" + email + ", fullName=" + fullName + "]";
    }
}
