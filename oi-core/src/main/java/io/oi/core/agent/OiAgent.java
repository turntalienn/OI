package io.oi.core.agent;

import io.oi.core.config.OiCoreProperties;
import io.oi.core.emitter.HttpFlowEmitter;
import io.oi.core.trace.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.lang.instrument.Instrumentation;

public class OiAgent {

    private static final Logger log = LoggerFactory.getLogger(OiAgent.class);

    public static void premain(String agentArgs, Instrumentation inst) {
        log.info("Starting OI-Core Agent...");

        // In a real application, you'd load properties from a file
        // or system properties. For now, we use defaults.
        OiCoreProperties properties = new OiCoreProperties();

        if (!properties.isEnabled()) {
            log.info("OI-Core Agent is disabled by configuration.");
            return;
        }

        HttpFlowEmitter emitter = new HttpFlowEmitter(properties.getEmitter());
        Tracer.setEmitter(emitter);

        OiClassFileTransformer transformer = new OiClassFileTransformer(properties);
        inst.addTransformer(transformer);

        log.info("OI-Core Agent started successfully.");
    }
} 