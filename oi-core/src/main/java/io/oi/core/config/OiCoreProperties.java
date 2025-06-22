package io.oi.core.config;

import java.util.List;
import java.util.ArrayList;

/**
 * Configuration properties for the OI-Core agent.
 * <p>
 * This class holds all the settings that control the agent's behavior,
 * including instrumentation targets, emitter configuration, and feature flags.
 * It is designed to be populated by a configuration framework like Spring's
 * {@code @ConfigurationProperties} but can be used directly.
 */
public class OiCoreProperties {

    /**
     * Globally enables or disables the OI-Core agent. If false, no instrumentation will occur.
     */
    private boolean enabled = true;
    /**
     * List of package prefixes to be included for instrumentation.
     * Classes within these packages will be considered for transformation.
     */
    private List<String> includePackages = new ArrayList<>(List.of("com.mycompany"));
    /**
     * List of fully-qualified annotation names. Classes annotated with any of these will be excluded
     * from instrumentation.
     */
    private List<String> excludeAnnotations = new ArrayList<>(List.of("org.springframework.stereotype.Component"));
    /**
     * Configuration for the data emitter.
     */
    private EmitterProperties emitter = new EmitterProperties();
    /**
     * Fine-grained control over which types of instrumentation are active.
     */
    private InstrumentationProperties instrumentation = new InstrumentationProperties();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public List<String> getIncludePackages() {
        return includePackages;
    }

    public void setIncludePackages(List<String> includePackages) {
        this.includePackages = includePackages;
    }

    public List<String> getExcludeAnnotations() {
        return excludeAnnotations;
    }

    public void setExcludeAnnotations(List<String> excludeAnnotations) {
        this.excludeAnnotations = excludeAnnotations;
    }

    public EmitterProperties getEmitter() {
        return emitter;
    }

    public void setEmitter(EmitterProperties emitter) {
        this.emitter = emitter;
    }

    public InstrumentationProperties getInstrumentation() {
        return instrumentation;
    }

    public void setInstrumentation(InstrumentationProperties instrumentation) {
        this.instrumentation = instrumentation;
    }

    /**
     * Configuration for the {@link io.oi.core.emitter.FlowEmitter}.
     */
    public static class EmitterProperties {
        /**
         * The target URL for the HTTP emitter to POST FlowTree data to.
         */
        private String url = "http://localhost:8081/ingest";
        /**
         * The connection and request timeout in milliseconds for the HTTP emitter.
         */
        private int timeoutMs = 200;

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public int getTimeoutMs() {
            return timeoutMs;
        }

        public void setTimeoutMs(int timeoutMs) {
            this.timeoutMs = timeoutMs;
        }
    }

    /**
     * Provides flags to enable or disable specific instrumentation features.
     */
    public static class InstrumentationProperties {
        /**
         * Enable instrumentation for methods in Spring {@code @Controller} or {@code @RestController} classes.
         */
        private boolean controller = true;
        /**
         * Enable instrumentation for methods in Spring {@code @Service} classes.
         */
        private boolean service = true;
        /**
         * Enable instrumentation for methods in Spring {@code @Repository} classes.
         */
        private boolean repository = true;
        /**
         * Enable instrumentation of {@code java.lang.Thread.start()} calls.
         */
        private boolean threads = true;
        /**
         * Enable experimental AST parsing to gather conditional branch data. Requires source on classpath.
         */
        private boolean ast = true;
        /**
         * Enable collection of bytecode metrics (instruction count, local variables).
         */
        private boolean bytecode = true;
        /**
         * Enable tracing of Spring framework internal packages. (Used by oi-spring-adapter).
         */
        private boolean framework = false; // Corresponds to oi-spring-adapter setting

        public boolean isController() {
            return controller;
        }

        public void setController(boolean controller) {
            this.controller = controller;
        }

        public boolean isService() {
            return service;
        }

        public void setService(boolean service) {
            this.service = service;
        }

        public boolean isRepository() {
            return repository;
        }

        public void setRepository(boolean repository) {
            this.repository = repository;
        }

        public boolean isThreads() {
            return threads;
        }

        public void setThreads(boolean threads) {
            this.threads = threads;
        }

        public boolean isAst() {
            return ast;
        }

        public void setAst(boolean ast) {
            this.ast = ast;
        }

        public boolean isBytecode() {
            return bytecode;
        }

        public void setBytecode(boolean bytecode) {
            this.bytecode = bytecode;
        }

        public boolean isFramework() {
            return framework;
        }

        public void setFramework(boolean framework) {
            this.framework = framework;
        }
    }
} 