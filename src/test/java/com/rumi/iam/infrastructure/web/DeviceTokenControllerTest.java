package com.rumi.iam.infrastructure.web;

import com.rumi.iam.UserFixtures;
import com.rumi.iam.application.DeviceTokenApplicationService;
import com.rumi.iam.application.RegisteredDeviceToken;
import com.rumi.iam.domain.model.DevicePlatform;
import com.rumi.iam.domain.model.DeviceToken;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.Instant;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DeviceTokenController.class)
class DeviceTokenControllerTest {

    private static final String DEVICE_TOKENS = "/api/v1/device-tokens";
    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String FIREBASE_TOKEN = "fcm-token-of-the-phone:APA91bH";
    private static final UUID DEVICE_ID = UUID.fromString("5c1d7e9a-3b2f-4d6e-8a1c-9f0e2d4b6a8c");
    private static final Instant REGISTERED_AT = Instant.parse("2026-10-29T15:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DeviceTokenApplicationService deviceTokenService;

    private static RegisteredDeviceToken registered(boolean created) {
        DeviceToken device = new DeviceToken(DEVICE_ID, UserFixtures.USER_ID, FIREBASE_TOKEN, DevicePlatform.ANDROID,
                REGISTERED_AT, REGISTERED_AT);
        return new RegisteredDeviceToken(device, created);
    }

    @Test
    void registersANewDeviceAndAnswersCreatedWithoutTheToken() throws Exception {
        when(deviceTokenService.register(UserFixtures.USER_ID, FIREBASE_TOKEN, DevicePlatform.ANDROID))
                .thenReturn(registered(true));

        send(UserFixtures.USER_ID.toString(), json(FIREBASE_TOKEN, "ANDROID"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(DEVICE_ID.toString()))
                .andExpect(jsonPath("$.platform").value("ANDROID"))
                .andExpect(jsonPath("$.registeredAt").value("2026-10-29T15:00:00Z"))
                .andExpect(jsonPath("$.token").doesNotExist())
                .andExpect(content().string(not(containsString(FIREBASE_TOKEN))));
    }

    @Test
    void answersOkWithTheSameBodyForADeviceAlreadyRegistered() throws Exception {
        when(deviceTokenService.register(UserFixtures.USER_ID, FIREBASE_TOKEN, DevicePlatform.ANDROID))
                .thenReturn(registered(false));

        send(UserFixtures.USER_ID.toString(), json(FIREBASE_TOKEN, "ANDROID"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(DEVICE_ID.toString()))
                .andExpect(jsonPath("$.platform").value("ANDROID"))
                .andExpect(jsonPath("$.registeredAt").value("2026-10-29T15:00:00Z"));
    }

    @Test
    void rejectsMissingFields() throws Exception {
        expectBadRequest("{}")
                .andExpect(jsonPath("$.errors.token").value("must not be blank"))
                .andExpect(jsonPath("$.errors.platform").value("must not be null"));
        verifyNoInteractions(deviceTokenService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"BLACKBERRY", "android", ""})
    void rejectsAnUnknownPlatform(String platform) throws Exception {
        expectBadRequest(json(FIREBASE_TOKEN, platform))
                .andExpect(jsonPath("$.errors.platform").value("must be one of ANDROID, IOS, WEB"));
        verifyNoInteractions(deviceTokenService);
    }

    @Test
    void rejectsATokenLongerThanTheMaximumWithoutEchoingIt() throws Exception {
        String tooLong = "t".repeat(DeviceToken.MAX_TOKEN_LENGTH + 1);

        expectBadRequest(json(tooLong, "WEB"))
                .andExpect(jsonPath("$.errors.token").value("size must be between 0 and 1024"))
                .andExpect(content().string(not(containsString(tooLong))));
        verifyNoInteractions(deviceTokenService);
    }

    @Test
    void answersUnauthorizedWithoutTheGatewayHeader() throws Exception {
        mockMvc.perform(post(DEVICE_TOKENS).contentType(MediaType.APPLICATION_JSON).content(json(FIREBASE_TOKEN, "IOS")))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.title").value("Unauthorized"))
                .andExpect(jsonPath("$.detail").value(MissingUserIdException.MESSAGE))
                .andExpect(jsonPath("$.instance").value(DEVICE_TOKENS));
        verifyNoInteractions(deviceTokenService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "not-a-uuid", "12345"})
    void answersUnauthorizedWhenTheGatewayHeaderIsNotAUuid(String userId) throws Exception {
        send(userId, json(FIREBASE_TOKEN, "IOS"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value(MissingUserIdException.MESSAGE));
        verifyNoInteractions(deviceTokenService);
    }

    @Test
    void checksTheGatewayHeaderBeforeTheBody() throws Exception {
        mockMvc.perform(post(DEVICE_TOKENS).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }

    private ResultActions send(String userId, String body) throws Exception {
        return mockMvc.perform(post(DEVICE_TOKENS).header(USER_ID_HEADER, userId)
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private ResultActions expectBadRequest(String body) throws Exception {
        return send(UserFixtures.USER_ID.toString(), body)
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400));
    }

    private static String json(String token, String platform) {
        return """
                {"token":"%s","platform":"%s"}""".formatted(token, platform);
    }
}
