package com.landslide.usermanagement.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneOffset;

@Configuration
public class AppConfig {

    /** UTC clock — can be overridden in tests with a fixed Clock for quiet-hours testing */
    @Bean
    public Clock clock() {
        return Clock.system(ZoneOffset.UTC);
    }
}
