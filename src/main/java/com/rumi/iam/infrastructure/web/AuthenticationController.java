package com.rumi.iam.infrastructure.web;

import com.rumi.iam.application.AuthenticationApplicationService;
import com.rumi.iam.infrastructure.web.dto.AuthenticationResponse;
import com.rumi.iam.infrastructure.web.dto.ForgotPasswordRequest;
import com.rumi.iam.infrastructure.web.dto.ResetPasswordRequest;
import com.rumi.iam.infrastructure.web.dto.SignInRequest;
import com.rumi.iam.infrastructure.web.dto.SignUpRequest;
import com.rumi.iam.infrastructure.web.dto.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public endpoints behind the API gateway ({@code /api/v1/authentication/**} needs no token).
 */
@RestController
@RequestMapping("/api/v1/authentication")
@Tag(name = "Authentication",
        description = "Administrator sign-up (US01), sign-in (US02) and password recovery (US03)")
public class AuthenticationController {

    private final AuthenticationApplicationService authenticationService;

    public AuthenticationController(AuthenticationApplicationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    @Operation(
            summary = "Register an administrator",
            description = "US01. Creates a user with the ADMINISTRATOR role and no buildings. "
                    + "The password is stored only as a BCrypt hash. Sign in afterwards to get a token."
    )
    @ApiResponse(responseCode = "201", description = "The administrator was registered",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = UserResponse.class)))
    @ApiResponse(responseCode = "400", description = "Invalid data; field errors are listed in 'errors'",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "The email is already registered",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    @PostMapping("/sign-up")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse signUp(@Valid @RequestBody SignUpRequest request) {
        return UserResponse.from(authenticationService.signUp(request.email(), request.fullName(), request.password()));
    }

    @Operation(
            summary = "Sign in",
            description = "US02. Returns an HS256 JWT with the claims sub, role, buildingIds and exp, "
                    + "which the API gateway validates on every request."
    )
    @ApiResponse(responseCode = "200", description = "The credentials are valid",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = AuthenticationResponse.class)))
    @ApiResponse(responseCode = "400", description = "Missing email or password",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "401", description = "Unknown email or wrong password; the answer is the same for both",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    @PostMapping("/sign-in")
    public AuthenticationResponse signIn(@Valid @RequestBody SignInRequest request) {
        return AuthenticationResponse.from(authenticationService.signIn(request.email(), request.password()));
    }

    @Operation(
            summary = "Request a password reset link",
            description = "US03. If the email belongs to a user, a single-use link valid for 30 minutes is sent and "
                    + "earlier unused links stop working. The answer is always 202, so it does not reveal whether "
                    + "the email is registered. While Rumi has no mail sender, the link is only written to the "
                    + "service log (development stand-in)."
    )
    @ApiResponse(responseCode = "202", description = "Accepted; a link is sent if the email is registered. Empty body",
            content = @Content)
    @ApiResponse(responseCode = "400", description = "Missing or malformed email",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class),
                    examples = @ExampleObject(name = "Malformed email", value = """
                            {"type":"about:blank","title":"Bad Request","status":400,\
                            "detail":"Invalid request content.","instance":"/api/v1/authentication/forgot-password",\
                            "errors":{"email":"must be a well-formed email address"}}""")))
    @PostMapping("/forgot-password")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authenticationService.requestPasswordReset(request.email());
    }

    @Operation(
            summary = "Reset the password with a link",
            description = "US03. Sets a new password with the token of a reset link. The link works once and for "
                    + "30 minutes; afterwards every other unused link of the user stops working too. Tokens issued "
                    + "before the reset stay valid until they expire (15 minutes)."
    )
    @ApiResponse(responseCode = "204", description = "The password was changed. Empty body", content = @Content)
    @ApiResponse(responseCode = "400",
            description = "Invalid data (field errors in 'errors'), or an unknown, expired or already used link",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class),
                    examples = {
                            @ExampleObject(name = "Invalid or expired link", value = """
                                    {"type":"about:blank","title":"Bad Request","status":400,\
                                    "detail":"Invalid or expired token.",\
                                    "instance":"/api/v1/authentication/reset-password"}"""),
                            @ExampleObject(name = "Weak password", value = """
                                    {"type":"about:blank","title":"Bad Request","status":400,\
                                    "detail":"Invalid request content.",\
                                    "instance":"/api/v1/authentication/reset-password",\
                                    "errors":{"newPassword":"Password must have at least 8 characters and at most 72 bytes"}}""")
                    }))
    @PostMapping("/reset-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authenticationService.resetPassword(request.token(), request.newPassword());
    }
}
