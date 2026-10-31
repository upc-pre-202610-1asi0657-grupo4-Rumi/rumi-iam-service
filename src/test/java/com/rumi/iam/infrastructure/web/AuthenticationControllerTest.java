package com.rumi.iam.infrastructure.web;

import com.rumi.iam.UserFixtures;
import com.rumi.iam.application.AuthenticatedUser;
import com.rumi.iam.application.AuthenticationApplicationService;
import com.rumi.iam.application.EmailAlreadyRegisteredException;
import com.rumi.iam.application.InvalidCredentialsException;
import com.rumi.iam.domain.model.PasswordPolicy;
import com.rumi.iam.domain.service.IssuedToken;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.Instant;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthenticationController.class)
class AuthenticationControllerTest {

    private static final String SIGN_UP = "/api/v1/authentication/sign-up";
    private static final String SIGN_IN = "/api/v1/authentication/sign-in";
    private static final String TOKEN = "header.payload.signature";
    private static final Instant EXPIRES_AT = Instant.parse("2026-10-29T16:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthenticationApplicationService authenticationService;

    @Test
    void signsUpAnAdministratorAndAnswersCreatedWithoutPasswordData() throws Exception {
        when(authenticationService.signUp(UserFixtures.EMAIL, UserFixtures.FULL_NAME, UserFixtures.PASSWORD))
                .thenReturn(UserFixtures.administrator());

        mockMvc.perform(post(SIGN_UP).contentType(MediaType.APPLICATION_JSON)
                        .content(signUpJson(UserFixtures.EMAIL, UserFixtures.PASSWORD, UserFixtures.FULL_NAME)))
                .andExpect(status().isCreated())
                .andExpect(header().doesNotExist(HttpHeaders.LOCATION))
                .andExpect(jsonPath("$.id").value(UserFixtures.USER_ID.toString()))
                .andExpect(jsonPath("$.email").value(UserFixtures.EMAIL))
                .andExpect(jsonPath("$.fullName").value(UserFixtures.FULL_NAME))
                .andExpect(jsonPath("$.role").value("ADMINISTRATOR"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(content().string(not(containsString(UserFixtures.PASSWORD_HASH))));
    }

    @Test
    void rejectsMissingSignUpFieldsWithABadRequestProblemDetail() throws Exception {
        expectBadRequest(SIGN_UP, "{}")
                .andExpect(jsonPath("$.instance").value(SIGN_UP))
                .andExpect(jsonPath("$.errors.email").value("must not be blank"))
                .andExpect(jsonPath("$.errors.password").value("must not be null"))
                .andExpect(jsonPath("$.errors.fullName").value("must not be blank"));
        verifyNoInteractions(authenticationService);
    }

    @Test
    void rejectsAMalformedEmail() throws Exception {
        expectBadRequest(SIGN_UP, signUpJson("not-an-email", UserFixtures.PASSWORD, UserFixtures.FULL_NAME))
                .andExpect(jsonPath("$.errors.email").value("must be a well-formed email address"));
        verifyNoInteractions(authenticationService);
    }

    @Test
    void rejectsAPasswordWithFewerThanEightCharactersEvenIfItHasEightBytes() throws Exception {
        expectBadRequest(SIGN_UP, signUpJson(UserFixtures.EMAIL, "ñ".repeat(4), UserFixtures.FULL_NAME))
                .andExpect(jsonPath("$.errors.password").value(PasswordPolicy.MESSAGE));
        verifyNoInteractions(authenticationService);
    }

    @Test
    void rejectsAPasswordLongerThanSeventyTwoBytes() throws Exception {
        expectBadRequest(SIGN_UP, signUpJson(UserFixtures.EMAIL, "ñ".repeat(37), UserFixtures.FULL_NAME))
                .andExpect(jsonPath("$.errors.password").value(PasswordPolicy.MESSAGE));
        verifyNoInteractions(authenticationService);
    }

    @Test
    void neverEchoesTheRejectedPassword() throws Exception {
        String password = "tiny";

        expectBadRequest(SIGN_UP, signUpJson(UserFixtures.EMAIL, password, UserFixtures.FULL_NAME))
                .andExpect(content().string(not(containsString("\"" + password + "\""))));
    }

    @Test
    void rejectsAFullNameLongerThanTheMaximum() throws Exception {
        expectBadRequest(SIGN_UP, signUpJson(UserFixtures.EMAIL, UserFixtures.PASSWORD, "a".repeat(101)))
                .andExpect(jsonPath("$.errors.fullName").value("size must be between 0 and 100"));
    }

    @Test
    void answersConflictWhenTheEmailIsAlreadyRegistered() throws Exception {
        when(authenticationService.signUp(any(), any(), any())).thenThrow(new EmailAlreadyRegisteredException());

        mockMvc.perform(post(SIGN_UP).contentType(MediaType.APPLICATION_JSON)
                        .content(signUpJson(UserFixtures.EMAIL, UserFixtures.PASSWORD, UserFixtures.FULL_NAME)))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.title").value("Conflict"))
                .andExpect(jsonPath("$.detail").value("An account with this email already exists."))
                .andExpect(jsonPath("$.instance").value(SIGN_UP));
    }

    @Test
    void answersBadRequestWhenTheDomainRejectsTheData() throws Exception {
        when(authenticationService.signUp(any(), any(), any()))
                .thenThrow(new IllegalArgumentException("Email is not a valid address"));

        expectBadRequest(SIGN_UP, signUpJson("ana@rumi", UserFixtures.PASSWORD, UserFixtures.FULL_NAME))
                .andExpect(jsonPath("$.detail").value("Email is not a valid address"));
    }

    @Test
    void rejectsAMalformedBody() throws Exception {
        expectBadRequest(SIGN_UP, "{not json")
                .andExpect(jsonPath("$.detail").value("Failed to read request"));
    }

    @Test
    void signsInAndReturnsABearerToken() throws Exception {
        when(authenticationService.signIn(UserFixtures.EMAIL, UserFixtures.PASSWORD)).thenReturn(
                new AuthenticatedUser(UserFixtures.administrator(), new IssuedToken(TOKEN, EXPIRES_AT)));

        mockMvc.perform(post(SIGN_IN).contentType(MediaType.APPLICATION_JSON)
                        .content(signInJson(UserFixtures.EMAIL, UserFixtures.PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(UserFixtures.USER_ID.toString()))
                .andExpect(jsonPath("$.email").value(UserFixtures.EMAIL))
                .andExpect(jsonPath("$.role").value("ADMINISTRATOR"))
                .andExpect(jsonPath("$.token").value(TOKEN))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresAt").value("2026-10-29T16:00:00Z"))
                .andExpect(content().string(not(containsString(UserFixtures.PASSWORD_HASH))));
    }

    @Test
    void answersUnauthorizedForInvalidCredentials() throws Exception {
        when(authenticationService.signIn(any(), any())).thenThrow(new InvalidCredentialsException());

        mockMvc.perform(post(SIGN_IN).contentType(MediaType.APPLICATION_JSON)
                        .content(signInJson(UserFixtures.EMAIL, "wrong-password")))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.title").value("Unauthorized"))
                .andExpect(jsonPath("$.detail").value("Invalid email or password."))
                .andExpect(jsonPath("$.instance").value(SIGN_IN));
    }

    @Test
    void rejectsMissingSignInFields() throws Exception {
        expectBadRequest(SIGN_IN, "{}")
                .andExpect(jsonPath("$.errors.email").value("must not be blank"))
                .andExpect(jsonPath("$.errors.password").value("must not be blank"));
        verifyNoInteractions(authenticationService);
    }

    private ResultActions expectBadRequest(String path, String body) throws Exception {
        return mockMvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.title").value("Bad Request"));
    }

    private static String signUpJson(String email, String password, String fullName) {
        return """
                {"email":"%s","password":"%s","fullName":"%s"}""".formatted(email, password, fullName);
    }

    private static String signInJson(String email, String password) {
        return """
                {"email":"%s","password":"%s"}""".formatted(email, password);
    }
}
