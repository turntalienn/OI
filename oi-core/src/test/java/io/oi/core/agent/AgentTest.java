package io.oi.core.agent;

import io.oi.core.config.OiCoreProperties;
import io.oi.core.emitter.FlowEmitter;
import io.oi.core.model.FlowTree;
import io.oi.core.trace.Tracer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class AgentTest {

    private TestFlowEmitter testEmitter;

    @BeforeEach
    void setUp() {
        testEmitter = new TestFlowEmitter();
        Tracer.setEmitter(testEmitter);
    }

    @AfterEach
    void tearDown() {
        Tracer.setEmitter(null);
    }

    @Test
    void testTracerBasicFunctionality() {
        // Test that the tracer can be set and retrieved
        assertNotNull(Tracer.getCurrentCallStack());
        assertTrue(Tracer.getCurrentCallStack().isEmpty());
    }

    @Test
    void testClassFileTransformerCreation() {
        OiCoreProperties properties = new OiCoreProperties();
        properties.getIncludePackages().add("io.oi.core.agent");
        
        OiClassFileTransformer transformer = new OiClassFileTransformer(properties);
        assertNotNull(transformer);
    }

    @Test
    void testPropertiesConfiguration() {
        OiCoreProperties properties = new OiCoreProperties();
        
        // Test default values
        assertTrue(properties.isEnabled());
        assertNotNull(properties.getIncludePackages());
        assertNotNull(properties.getEmitter());
        assertNotNull(properties.getInstrumentation());
        
        // Test setters
        properties.setEnabled(false);
        assertFalse(properties.isEnabled());
        
        properties.getIncludePackages().add("com.test");
        assertTrue(properties.getIncludePackages().contains("com.test"));
    }

    @Test
    void testEmitterFunctionality() {
        FlowTree testTree = new FlowTree("test-123", null);
        testEmitter.emit(testTree);
        
        assertEquals(1, testEmitter.getReceivedTrees().size());
        assertEquals(testTree, testEmitter.getReceivedTrees().get(0));
    }

    @Test
    void testNoInstrumentationForExcludedClass() throws Exception {
        OiCoreProperties properties = new OiCoreProperties();
        properties.getIncludePackages().add("io.oi.core.agent");
        
        OiClassFileTransformer transformer = new OiClassFileTransformer(properties);
        
        String excludedClassName = "java.lang.String"; // Should not be instrumented
        byte[] originalBytecode = new byte[100]; // Dummy bytecode
        
        byte[] transformedBytecode = transformer.transform(
                getClass().getClassLoader(), excludedClassName, null, null, originalBytecode);

        // Should return original bytecode unchanged for excluded classes
        assertArrayEquals(originalBytecode, transformedBytecode);
    }

    // A mock emitter to capture the FlowTree for assertions
    private static class TestFlowEmitter implements FlowEmitter {
        private final List<FlowTree> receivedTrees = new ArrayList<>();
        @Override
        public void emit(FlowTree tree) {
            receivedTrees.add(tree);
        }
        public List<FlowTree> getReceivedTrees() {
            return receivedTrees;
        }
    }
} 