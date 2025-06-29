package io.oi.spring.adapter.web;

import io.oi.core.config.OiCoreProperties;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Component
public class OiCoreHealthIndicator implements HealthIndicator {

    private final OiCoreProperties properties;
    private final HttpClient httpClient;

    public OiCoreHealthIndicator(OiCoreProperties properties) {
        this.properties = properties;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    @Override
    public Health health() {
        if (!properties.isEnabled()) {
            return Health.down()
                    .withDetail("reason", "oi-core agent is disabled in configuration")
                    .withDetail("enabled", false)
                    .build();
        }

        // Check if emitter URL is configured
        String emitterUrl = properties.getEmitter().getUrl();
        if (emitterUrl == null || emitterUrl.trim().isEmpty()) {
            return Health.down()
                    .withDetail("reason", "emitter URL is not configured")
                    .withDetail("enabled", true)
                    .withDetail("emitterUrl", "not configured")
                    .build();
        }

        // Try to ping the emitter endpoint
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(emitterUrl))
                    .timeout(Duration.ofSeconds(3))
                    .method("HEAD", HttpRequest.BodyPublishers.noBody())
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return Health.up()
                        .withDetail("enabled", true)
                        .withDetail("emitterUrl", emitterUrl)
                        .withDetail("emitterStatus", "reachable")
                        .withDetail("responseCode", response.statusCode())
                        .build();
            } else {
                return Health.down()
                        .withDetail("enabled", true)
                        .withDetail("emitterUrl", emitterUrl)
                        .withDetail("emitterStatus", "unreachable")
                        .withDetail("responseCode", response.statusCode())
                        .withDetail("reason", "emitter returned non-success status code")
                        .build();
            }
        } catch (Exception e) {
            return Health.down()
                    .withDetail("enabled", true)
                    .withDetail("emitterUrl", emitterUrl)
                    .withDetail("emitterStatus", "unreachable")
                    .withDetail("reason", "failed to connect to emitter: " + e.getMessage())
                    .build();
        }
    }
} 