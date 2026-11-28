package com.rumi.iam.infrastructure.web;

import com.rumi.iam.UserFixtures;
import com.rumi.iam.application.AuthenticatedUser;
import com.rumi.iam.application.InvalidAccessTokenException;
import com.rumi.iam.application.SessionExpiredException;
import com.rumi.iam.application.TokenReissueApplicationService;
import com.rumi.iam.domain.service.IssuedToken;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CurrentUserTokenController.class)
class CurrentUserTokenControllerTest {

    private static final String REISSUE = "/api/v1/users/me/token";
    private static final String CURRENT_TOKEN = "current.access.token";
    private static final UUID BUILDING_ID = UUID.fromString("7a9b3c1d-2e4f-4b6a-8c0d-1e2f3a4b5c6d");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TokenReissueApplicationService tokenReissueService;

    @Test
    void returnsANewTokenWithTheBuildingsOfTheUser() throws Exception {
        when(tokenReissueService.reissue(UserFixtures.USER_ID, CURRENT_TOKEN)).thenReturn(new AuthenticatedUser(
                UserFixtures.administrator().linkBuilding(BUILDING_ID),
                new IssuedToken("new.access.token", Instant.parse("2026-11-28T06:15:00Z"))));

        mockMvc.perform(post(REISSUE)
                        .header("X-User-Id", UserFixtures.USER_ID.toString())
                        .header("Authorization", "Bearer " + CURRENT_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(UserFixtures.USER_ID.toString()))
                .andExpect(jsonPath("$.token").value("new.access.token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.buildingIds[0]").value(BUILDING_ID.toString()));
    }

    @Test
    void answersUnauthorizedWithoutTheUserOfTheGateway() throws Exception {
        mockMvc.perform(post(REISSUE).header("Authorization", "Bearer " + CURRENT_TOKEN))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(tokenReissueService);
    }

    @Test
    void answersUnauthorizedWithoutABearerToken() throws Exception {
        mockMvc.perform(post(REISSUE).header("X-User-Id", UserFixtures.USER_ID.toString()))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("The access token is not valid"));

        verifyNoInteractions(tokenReissueService);
    }

    @Test
    void answersUnauthorizedForAnInvalidToken() throws Exception {
        when(tokenReissueService.reissue(UserFixtures.USER_ID, CURRENT_TOKEN))
                .thenThrow(new InvalidAccessTokenException());

        mockMvc.perform(post(REISSUE)
                        .header("X-User-Id", UserFixtures.USER_ID.toString())
                        .header("Authorization", "Bearer " + CURRENT_TOKEN))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void answersUnauthorizedTwelveHoursAfterTheSignIn() throws Exception {
        when(tokenReissueService.reissue(UserFixtures.USER_ID, CURRENT_TOKEN))
                .thenThrow(new SessionExpiredException(Duration.ofHours(12)));

        mockMvc.perform(post(REISSUE)
                        .header("X-User-Id", UserFixtures.USER_ID.toString())
                        .header("Authorization", "Bearer " + CURRENT_TOKEN))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("The session started more than 12 hours ago; sign in again"));
    }
}
