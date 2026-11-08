package com.yassinecharit.budget.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Swagger UI with a "Authorize" button for the JWT token. */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI smartBudgetOpenApi() {
        return new OpenAPI()
                .info(new Info().title("Smart Budget API").version("1.0.0")
                        .description("Personal budget management: CSV bank import, AI categorization, budgets, dashboard"))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .components(new Components().addSecuritySchemes("bearerAuth",
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")));
    }
}
