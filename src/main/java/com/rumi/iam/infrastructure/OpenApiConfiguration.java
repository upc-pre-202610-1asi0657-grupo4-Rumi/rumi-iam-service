package com.rumi.iam.infrastructure;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfiguration {

    @Bean
    public OpenAPI iamServiceOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Rumi Iam Service API")
                .description("REST API of the Identity & Access Management bounded context. "
                        + "Skeleton: only the health endpoint exists; functionality is planned for later sprints.")
                .version("0.1.0"));
    }
}
