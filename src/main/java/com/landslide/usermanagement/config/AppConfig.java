package com.landslide.usermanagement.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Clock;
import java.time.ZoneOffset;

@Configuration
public class AppConfig {

    @Bean
    public Clock clock() {
        return Clock.system(ZoneOffset.UTC);
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
