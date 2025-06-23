package com.aims.core;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * Spring Boot main application class for AIMS Web API
 */
@SpringBootApplication
@ComponentScan(basePackages = {
    "com.aims.core.rest",
    "com.aims.core.application",
    "com.aims.core.infrastructure",
    "com.aims.core.shared"
})
public class AimsWebApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(AimsWebApiApplication.class, args);
    }
}