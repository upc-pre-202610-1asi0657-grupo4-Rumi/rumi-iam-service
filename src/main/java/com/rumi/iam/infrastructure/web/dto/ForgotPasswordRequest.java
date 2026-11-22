package com.rumi.iam.infrastructure.web.dto;

import com.rumi.iam.domain.model.EmailAddress;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(name = "ForgotPasswordRequest", description = "Email of the account whose password was forgotten")
public record ForgotPasswordRequest(
        @Schema(description = "Email of the user, in any case", example = "ana.torres@rumi.pe")
        @NotBlank @Email @Size(max = EmailAddress.MAX_LENGTH)
        String email
) {
}
