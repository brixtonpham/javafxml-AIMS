package com.aims.core.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.util.List;

/**
 * Web configuration for the Spring Boot application.
 * Handles static resources, message converters, and additional web settings.
 */
@Configuration
@EnableWebMvc
public class WebConfig implements WebMvcConfigurer {

    /**
     * Configure static resource handlers
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Serve static content from classpath
        registry.addResourceHandler("/static/**")
                .addResourceLocations("classpath:/static/");
        
        registry.addResourceHandler("/images/**")
                .addResourceLocations("classpath:/images/");
        
        registry.addResourceHandler("/css/**")
                .addResourceLocations("classpath:/css/");
        
        registry.addResourceHandler("/js/**")
                .addResourceLocations("classpath:/js/");

        // Swagger UI resources
        registry.addResourceHandler("/swagger-ui/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/swagger-ui/");
        
        registry.addResourceHandler("/webjars/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/");
    }

    /**
     * CORS configuration is handled by CorsConfig.java to avoid conflicts
     * This method intentionally left empty to prevent duplicate CORS configuration
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // CORS configuration is handled by CorsConfig.java
    }

    /**
     * Configure HTTP message converters
     */
    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        // Configure Jackson for JSON serialization
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        
        MappingJackson2HttpMessageConverter jsonConverter = new MappingJackson2HttpMessageConverter();
        jsonConverter.setObjectMapper(objectMapper);
        
        converters.add(jsonConverter);
    }
}