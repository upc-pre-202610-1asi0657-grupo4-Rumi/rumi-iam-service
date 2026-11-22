package com.rumi.iam.acceptance;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rumi.iam.MutableClock;
import com.rumi.iam.application.CapturingPasswordResetNotifier;
import io.cucumber.spring.ScenarioScope;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

/**
 * Test doubles of the acceptance scenarios: a clock they can move forward, a notifier that keeps the reset links
 * instead of logging them, and the per-scenario HTTP client.
 */
@TestConfiguration(proxyBeanMethods = false)
public class AcceptanceTestConfiguration {

    @Bean
    @Primary
    public MutableClock testClock() {
        return new MutableClock(Instant.now());
    }

    @Bean
    @Primary
    public CapturingPasswordResetNotifier capturingPasswordResetNotifier() {
        return new CapturingPasswordResetNotifier();
    }

    @Bean
    @ScenarioScope
    public ApiClient apiClient(MockMvc mockMvc, ObjectMapper objectMapper) {
        return new ApiClient(mockMvc, objectMapper);
    }
}
