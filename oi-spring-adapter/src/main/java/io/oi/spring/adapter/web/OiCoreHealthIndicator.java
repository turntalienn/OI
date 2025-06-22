package io.oi.spring.adapter.web;

import io.oi.core.config.OiCoreProperties;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class OiCoreHealthIndicator implements HealthIndicator {

    private final OiCoreProperties properties;

    public OiCoreHealthIndicator(OiCoreProperties properties) {
        this.properties = properties;
    }

    @Override
    public Health health() {
        if (!properties.isEnabled()) {
            return Health.down().withDetail("reason", "oi-core agent is disabled in configuration").build();
        }

        // The user also requested to ping the emitter endpoint.
        // This can be complex (e.g., requires a blocking call or async handling in a health check).
        // For now, we report UP if the agent is enabled. A future version could add the ping.
        return Health.up()
                .withDetail("enabled", true)
                .withDetail("emitterUrl", properties.getEmitter().getUrl())
                .build();
    }
} 