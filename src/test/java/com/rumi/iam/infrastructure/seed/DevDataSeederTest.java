package com.rumi.iam.infrastructure.seed;

import com.rumi.iam.PostgresTestContainer;
import com.rumi.iam.domain.model.EmailAddress;
import com.rumi.iam.domain.model.Role;
import com.rumi.iam.domain.repository.UserRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DevDataSeederTest {

    private static final String TEST_SECRET = "rumi.security.jwt.secret=test-only-secret-not-for-production-32b!";

    /**
     * Dev profile (H2): the demo users exist, already linked to the sample building, and can sign in.
     */
    @Nested
    @SpringBootTest(properties = TEST_SECRET)
    @AutoConfigureMockMvc
    @ActiveProfiles("dev")
    class DevProfile {

        @Autowired
        private UserRepository users;

        @Autowired
        private MockMvc mockMvc;

        @Test
        void seedsAnAdministratorAndAResidentOfTheSampleBuilding() {
            assertThat(users.findById(DevDataSeeder.ADMINISTRATOR_ID)).hasValueSatisfying(user -> {
                assertThat(user.getRole()).isEqualTo(Role.ADMINISTRATOR);
                assertThat(user.getBuildingIds()).containsExactly(DevDataSeeder.BUILDING_ID);
            });
            assertThat(users.findById(DevDataSeeder.RESIDENT_ID)).hasValueSatisfying(user -> {
                assertThat(user.getRole()).isEqualTo(Role.RESIDENT);
                assertThat(user.getBuildingIds()).containsExactly(DevDataSeeder.BUILDING_ID);
            });
        }

        @Test
        void theDemoAdministratorSignsInWithTheBuildingInTheResponse() throws Exception {
            mockMvc.perform(post("/api/v1/authentication/sign-in")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"" + DevDataSeeder.ADMINISTRATOR_EMAIL + "\",\"password\":\""
                                    + DevDataSeeder.DEMO_PASSWORD + "\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.buildingIds[0]").value(DevDataSeeder.BUILDING_ID.toString()));
        }
    }

    /**
     * Default profile (no profile active, as in Docker Compose): no demo user is created.
     */
    @Nested
    @SpringBootTest(properties = {TEST_SECRET, "spring.rabbitmq.listener.simple.auto-startup=false"})
    @Import(PostgresTestContainer.class)
    class DefaultProfile {

        @Autowired
        private ApplicationContext context;

        @Autowired
        private UserRepository users;

        @Test
        void doesNotLoadTheSeeder() {
            assertThat(context.getEnvironment().getActiveProfiles()).isEmpty();
            assertThat(context.getBeanNamesForType(DevDataSeeder.class)).isEmpty();
            assertThat(users.existsByEmail(new EmailAddress(DevDataSeeder.ADMINISTRATOR_EMAIL))).isFalse();
            assertThat(users.existsByEmail(new EmailAddress(DevDataSeeder.RESIDENT_EMAIL))).isFalse();
        }
    }
}
