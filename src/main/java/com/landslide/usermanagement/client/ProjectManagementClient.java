package com.landslide.usermanagement.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Component
public class ProjectManagementClient {

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public ProjectManagementClient(
            RestTemplate restTemplate,
            @Value("${services.project-management.base-url}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    public boolean projectExists(String projectId) {
        try {
            restTemplate.getForEntity(baseUrl + "/api/v1/projects/" + projectId, Void.class);
            return true;
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) return false;
            throw e;
        }
    }
}
