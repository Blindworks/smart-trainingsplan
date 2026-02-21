package com.trainingsplan;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.context.annotation.Bean;

import java.util.Arrays;

@SpringBootApplication
public class SmartTrainingsplanApplication {

    @Value("${app.cors.allowed-origins:http://localhost:4200}")
    private String corsAllowedOrigins;

    public static void main(String[] args) {
        SpringApplication.run(SmartTrainingsplanApplication.class, args);
    }

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        String[] allowedOrigins = Arrays.stream(corsAllowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .toArray(String[]::new);

        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                        .allowedOrigins(allowedOrigins)
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                        .allowedHeaders("*")
                        .allowCredentials(true);
            }
        };
    }
}
