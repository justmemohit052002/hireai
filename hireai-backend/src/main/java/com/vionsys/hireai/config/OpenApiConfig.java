package com.vionsys.hireai.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI hireAiOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("HireAI Platform REST API")
                        .description("Production-grade AI-powered Recruitment, ATS Resume Match Scoring, and Job Application Workflow Engine API.")
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("HireAI Engineering Team")
                                .email("support@hireai.vionsys.com")
                                .url("https://vionsys.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME,
                                new SecurityScheme()
                                        .name(SECURITY_SCHEME_NAME)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Enter your JWT token to authorize requests. Example: `eyJhbGciOiJIUzUxMiJ9...`")));
    }
}
