package io.oi.core.agent;

import io.oi.core.config.OiCoreProperties;
import io.oi.core.emitter.HttpFlowEmitter;
import io.oi.core.trace.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.lang.instrument.Instrumentation;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class OiAgent {

    private static final Logger log = LoggerFactory.getLogger(OiAgent.class);

    public static void premain(String agentArgs, Instrumentation inst) {
        log.info("Starting OI-Core Agent...");

        // Load properties from agent arguments, system properties, or defaults
        OiCoreProperties properties = loadProperties(agentArgs);

        if (!properties.isEnabled()) {
            log.info("OI-Core Agent is disabled by configuration.");
            return;
        }

        log.info("OI-Core Agent configuration: enabled={}, includePackages={}, emitterUrl={}", 
                properties.isEnabled(), 
                properties.getIncludePackages(), 
                properties.getEmitter().getUrl());

        HttpFlowEmitter emitter = new HttpFlowEmitter(properties.getEmitter());
        Tracer.setEmitter(emitter);

        OiClassFileTransformer transformer = new OiClassFileTransformer(properties);
        inst.addTransformer(transformer);

        log.info("OI-Core Agent started successfully.");
    }

    private static OiCoreProperties loadProperties(String agentArgs) {
        OiCoreProperties properties = new OiCoreProperties();
        
        // Try to load from agent arguments first
        if (agentArgs != null && !agentArgs.trim().isEmpty()) {
            try {
                loadPropertiesFromArgs(agentArgs, properties);
                log.info("Loaded properties from agent arguments: {}", agentArgs);
            } catch (Exception e) {
                log.warn("Failed to parse agent arguments: {}, using defaults", agentArgs, e);
            }
        }

        // Override with system properties
        loadPropertiesFromSystem(properties);

        // Try to load from config file if specified
        String configFile = System.getProperty("oi-core.config.file");
        if (configFile != null && !configFile.trim().isEmpty()) {
            try {
                loadPropertiesFromFile(configFile, properties);
                log.info("Loaded properties from config file: {}", configFile);
            } catch (Exception e) {
                log.warn("Failed to load config file: {}, using current properties", configFile, e);
            }
        }

        return properties;
    }

    private static void loadPropertiesFromArgs(String agentArgs, OiCoreProperties properties) {
        String[] args = agentArgs.split(",");
        for (String arg : args) {
            String[] keyValue = arg.split("=", 2);
            if (keyValue.length == 2) {
                String key = keyValue[0].trim();
                String value = keyValue[1].trim();
                
                switch (key) {
                    case "enabled":
                        properties.setEnabled(Boolean.parseBoolean(value));
                        break;
                    case "includePackages":
                        properties.getIncludePackages().clear();
                        for (String pkg : value.split(";")) {
                            properties.getIncludePackages().add(pkg.trim());
                        }
                        break;
                    case "emitter.url":
                        properties.getEmitter().setUrl(value);
                        break;
                    case "emitter.timeoutMs":
                        properties.getEmitter().setTimeoutMs(Integer.parseInt(value));
                        break;
                    case "instrumentation.framework":
                        properties.getInstrumentation().setFramework(Boolean.parseBoolean(value));
                        break;
                    case "instrumentation.service":
                        properties.getInstrumentation().setService(Boolean.parseBoolean(value));
                        break;
                    case "instrumentation.repository":
                        properties.getInstrumentation().setRepository(Boolean.parseBoolean(value));
                        break;
                    case "instrumentation.ast":
                        properties.getInstrumentation().setAst(Boolean.parseBoolean(value));
                        break;
                }
            }
        }
    }

    private static void loadPropertiesFromSystem(OiCoreProperties properties) {
        // Check system properties for overrides
        String enabled = System.getProperty("oi-core.enabled");
        if (enabled != null) {
            properties.setEnabled(Boolean.parseBoolean(enabled));
        }

        String includePackages = System.getProperty("oi-core.includePackages");
        if (includePackages != null) {
            properties.getIncludePackages().clear();
            for (String pkg : includePackages.split(",")) {
                properties.getIncludePackages().add(pkg.trim());
            }
        }

        String emitterUrl = System.getProperty("oi-core.emitter.url");
        if (emitterUrl != null) {
            properties.getEmitter().setUrl(emitterUrl);
        }

        String timeoutMs = System.getProperty("oi-core.emitter.timeoutMs");
        if (timeoutMs != null) {
            properties.getEmitter().setTimeoutMs(Integer.parseInt(timeoutMs));
        }

        String framework = System.getProperty("oi-core.instrumentation.framework");
        if (framework != null) {
            properties.getInstrumentation().setFramework(Boolean.parseBoolean(framework));
        }

        String service = System.getProperty("oi-core.instrumentation.service");
        if (service != null) {
            properties.getInstrumentation().setService(Boolean.parseBoolean(service));
        }

        String repository = System.getProperty("oi-core.instrumentation.repository");
        if (repository != null) {
            properties.getInstrumentation().setRepository(Boolean.parseBoolean(repository));
        }

        String ast = System.getProperty("oi-core.instrumentation.ast");
        if (ast != null) {
            properties.getInstrumentation().setAst(Boolean.parseBoolean(ast));
        }
    }

    private static void loadPropertiesFromFile(String configFile, OiCoreProperties properties) throws IOException {
        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream(configFile)) {
            props.load(fis);
        }

        // Map properties file keys to OiCoreProperties
        String enabled = props.getProperty("oi-core.enabled");
        if (enabled != null) {
            properties.setEnabled(Boolean.parseBoolean(enabled));
        }

        String includePackages = props.getProperty("oi-core.includePackages");
        if (includePackages != null) {
            properties.getIncludePackages().clear();
            for (String pkg : includePackages.split(",")) {
                properties.getIncludePackages().add(pkg.trim());
            }
        }

        String emitterUrl = props.getProperty("oi-core.emitter.url");
        if (emitterUrl != null) {
            properties.getEmitter().setUrl(emitterUrl);
        }

        String timeoutMs = props.getProperty("oi-core.emitter.timeoutMs");
        if (timeoutMs != null) {
            properties.getEmitter().setTimeoutMs(Integer.parseInt(timeoutMs));
        }

        String framework = props.getProperty("oi-core.instrumentation.framework");
        if (framework != null) {
            properties.getInstrumentation().setFramework(Boolean.parseBoolean(framework));
        }

        String service = props.getProperty("oi-core.instrumentation.service");
        if (service != null) {
            properties.getInstrumentation().setService(Boolean.parseBoolean(service));
        }

        String repository = props.getProperty("oi-core.instrumentation.repository");
        if (repository != null) {
            properties.getInstrumentation().setRepository(Boolean.parseBoolean(repository));
        }

        String ast = props.getProperty("oi-core.instrumentation.ast");
        if (ast != null) {
            properties.getInstrumentation().setAst(Boolean.parseBoolean(ast));
        }
    }
} 