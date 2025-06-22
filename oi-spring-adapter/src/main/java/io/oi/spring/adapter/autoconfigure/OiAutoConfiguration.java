package io.oi.spring.adapter.autoconfigure;

import io.oi.core.config.OiCoreProperties;
import io.oi.core.emitter.FlowEmitter;
import io.oi.core.emitter.HttpFlowEmitter;
import io.oi.core.trace.Tracer;
import io.oi.spring.adapter.aop.OiAspect;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@AutoConfiguration
@ConditionalOnClass(Tracer.class)
@ConditionalOnProperty(name = "oi-core.enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(OiCoreProperties.class)
public class OiAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(OiAutoConfiguration.class);

    private final OiCoreProperties properties;

    public OiAutoConfiguration(OiCoreProperties properties) {
        this.properties = properties;
    }

    @Bean
    public FlowEmitter flowEmitter() {
        return new HttpFlowEmitter(properties.getEmitter());
    }

    @Bean
    public OiAspect oiAspect() {
        return new OiAspect();
    }
    
    @PostConstruct
    public void initializeTracer() {
        log.info("Initializing OI-Spring-Adapter and setting FlowEmitter for OI-Core Tracer.");
        Tracer.setEmitter(flowEmitter());
    }
} 