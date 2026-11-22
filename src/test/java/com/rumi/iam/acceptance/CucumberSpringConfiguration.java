package com.rumi.iam.acceptance;

import com.rumi.iam.PostgresTestContainer;
import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * Starts the whole service on the shared PostgreSQL container of the tests.
 */
@CucumberContextConfiguration
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({PostgresTestContainer.class, AcceptanceTestConfiguration.class})
public class CucumberSpringConfiguration {
}
