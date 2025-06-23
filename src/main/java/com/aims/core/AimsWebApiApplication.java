package com.aims.core;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;

/**
 * Spring Boot main application class for AIMS Web API
 */
@EnableJpaRepositories(basePackages = "com.aims.core.infrastructure.database.dao")
@EntityScan(basePackages = "com.aims.core.entities")
@SpringBootApplication
@ComponentScan(basePackages = {
    "com.aims.core.rest",
    "com.aims.core.application",
    "com.aims.core.infrastructure",
    "com.aims.core.shared",
    "com.aims.core.config"
})
public class AimsWebApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(AimsWebApiApplication.class, args);
    }
}