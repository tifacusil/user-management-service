package com.landslide.usermanagement.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    private static final String BEARER_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("User Management Service")
                        .description("""
                                Microservice responsible for user authentication, profile management,
                                alert subscriptions, and device token registration in the
                                **Landslide Monitoring System**.

                                All endpoints except `/health` and `/api/v1/auth/**` require a JWT.
                                Click **Authorize** and enter: `Bearer <your-token>`
                                """)
                        .version("1.0.0"))
                .servers(List.of(
                        new Server().url("http://localhost:8085").description("Local"),
                        new Server().url("http://204.168.203.32:8085").description("Production")
                ))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME))
                .components(new Components()
                        .addSecuritySchemes(BEARER_SCHEME,
                                new SecurityScheme()
                                        .name(BEARER_SCHEME)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Paste the JWT token obtained from POST /api/v1/auth/login")));
    }
}
