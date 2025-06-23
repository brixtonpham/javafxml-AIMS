package com.aims.core.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;

import java.util.List;

/**
 * Swagger/OpenAPI 3 configuration for API documentation.
 * Provides interactive API documentation accessible at /swagger-ui/index.html
 */
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI aimsOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("AIMS Web API")
                        .description("RESTful API for the AIMS (Advanced Inventory Management System) web application")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("AIMS Development Team")
                                .email("support@aims.com")
                                .url("https://aims.com"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8080")
                                .description("Development server"),
                        new Server()
                                .url("http://localhost:3000")
                                .description("Frontend development server")))
                .tags(List.of(
                        new Tag()
                                .name("Authentication")
                                .description("User authentication and session management"),
                        new Tag()
                                .name("Products")
                                .description("Product catalog and search operations"),
                        new Tag()
                                .name("Cart")
                                .description("Shopping cart management"),
                        new Tag()
                                .name("Orders")
                                .description("Order processing and management"),
                        new Tag()
                                .name("Users")
                                .description("User account management (Admin operations)"),
                        new Tag()
                                .name("Payments")
                                .description("Payment processing and transaction management"),
                        new Tag()
                                .name("Admin Products")
                                .description("Administrative product management operations")
                ));
    }
}