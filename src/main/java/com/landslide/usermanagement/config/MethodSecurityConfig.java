package com.landslide.usermanagement.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

@Configuration
@EnableMethodSecurity
public class MethodSecurityConfig {
    // Enables @PreAuthorize, @PostAuthorize, etc. if needed later
}
