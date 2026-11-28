package com.rumi.iam.infrastructure.web;

import com.rumi.iam.application.InvalidAccessTokenException;
import com.rumi.iam.application.TokenReissueApplicationService;
import com.rumi.iam.infrastructure.web.dto.AuthenticationResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users/me")
@Tag(name = "Current user", description = "Operations of the signed-in user")
public class CurrentUserTokenController {

    private static final String BEARER_PREFIX = "Bearer ";

    private final TokenReissueApplicationService tokenReissueService;

    public CurrentUserTokenController(TokenReissueApplicationService tokenReissueService) {
        this.tokenReissueService = tokenReissueService;
    }

    @Operation(
            summary = "Reissue the token of the signed-in user",
            description = "Returns a new token with the buildings IAM knows now. Call it after registering a "
                    + "building: IAM links the building asynchronously, so retry a few times (for example every "
                    + "500 ms, up to 5 s) until buildingIds contains the new building. The new token keeps the "
                    + "auth_time of the password sign-in; 12 hours after it the answer is 401 and the user must "
                    + "sign in again. Send the current token as 'Authorization: Bearer <token>' through the gateway."
    )
    @ApiResponse(responseCode = "200", description = "A new token for the same user",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = AuthenticationResponse.class)))
    @ApiResponse(responseCode = "401",
            description = "No signed-in user, the token is not valid or belongs to another user, or the session "
                    + "started more than 12 hours ago",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class),
                    examples = {
                            @ExampleObject(name = "Invalid token", value = """
                                    {"type":"about:blank","title":"Unauthorized","status":401,
                                     "detail":"The access token is not valid","instance":"/api/v1/users/me/token"}"""),
                            @ExampleObject(name = "Session too old", value = """
                                    {"type":"about:blank","title":"Unauthorized","status":401,
                                     "detail":"The session started more than 12 hours ago; sign in again",
                                     "instance":"/api/v1/users/me/token"}""")
                    }))
    @PostMapping("/token")
    public AuthenticationResponse reissueToken(
            @Parameter(hidden = true) @CurrentUserId UUID userId,
            @Parameter(hidden = true) @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false)
            String authorization
    ) {
        return AuthenticationResponse.from(tokenReissueService.reissue(userId, bearerToken(authorization)));
    }

    private static String bearerToken(String authorization) {
        if (authorization == null || !authorization.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
            throw new InvalidAccessTokenException();
        }
        return authorization.substring(BEARER_PREFIX.length()).trim();
    }
}
