package com.adityax.homeservices.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI homeServicesOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Home Services Booking API")
                        .description("REST API for browsing home services and managing bookings")
                        .version("1.0.0"));
    }
}