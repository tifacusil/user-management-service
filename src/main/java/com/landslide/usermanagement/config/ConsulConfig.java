package com.landslide.usermanagement.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.web.client.RestTemplate;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Registers this service with Consul at startup using the HTTP agent API.
 * Pattern mirrors project-management and data-management services.
 */
@Configuration
public class ConsulConfig {

    private static final Logger log = LoggerFactory.getLogger(ConsulConfig.class);

    @Value("${consul.host:204.168.203.32}")
    private String consulHost;

    @Value("${consul.port:8500}")
    private int consulPort;

    @Value("${consul.service-host:}")
    private String configuredServiceHost;

    @Value("${server.port:8085}")
    private int serverPort;

    @Value("${app.env:development}")
    private String appEnv;

    @EventListener(ApplicationReadyEvent.class)
    public void registerWithConsul() {
        String serviceHost = resolveServiceHost();
        String healthCheckUrl = "http://" + serviceHost + ":" + serverPort + "/health";

        Map<String, Object> check = new HashMap<>();
        check.put("HTTP", healthCheckUrl);
        check.put("Interval", "10s");
        check.put("DeregisterCriticalServiceAfter", "1m");

        Map<String, Object> registration = new HashMap<>();
        registration.put("ID", "user-management-01");
        registration.put("Name", "user-management");
        registration.put("Port", serverPort);
        registration.put("Tags", List.of("version:1.0", "lang:java", "env:" + appEnv));
        registration.put("Check", check);

        String url = "http://" + consulHost + ":" + consulPort + "/v1/agent/service/register";

        try {
            RestTemplate restTemplate = new RestTemplate();
            restTemplate.put(url, registration);
            log.info("Registered with Consul — health check at {}", healthCheckUrl);
        } catch (Exception e) {
            log.warn("Could not register with Consul at {}:{} — {}", consulHost, consulPort, e.getMessage());
        }
    }

    private String resolveServiceHost() {
        if (configuredServiceHost != null && !configuredServiceHost.isBlank()) {
            return configuredServiceHost;
        }
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            return "localhost";
        }
    }
}
