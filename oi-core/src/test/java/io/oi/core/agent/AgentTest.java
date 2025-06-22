package io.oi.core.agent;

import io.oi.core.config.OiCoreProperties;
import io.oi.core.emitter.FlowEmitter;
import io.oi.core.model.FlowTree;
import io.oi.core.trace.Tracer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class AgentTest {

    private OiClassFileTransformer transformer;
    private TestFlowEmitter testEmitter;

    @BeforeEach
    void setUp() {
        OiCoreProperties properties = new OiCoreProperties();
        // Configure properties to instrument our test service
        properties.getIncludePackages().clear();
        properties.getIncludePackages().add("io.oi.core.agent");
        properties.getInstrumentation().setService(true);
        properties.getInstrumentation().setAst(false); // Disable for this test

        transformer = new OiClassFileTransformer(properties);
        testEmitter = new TestFlowEmitter();
        Tracer.setEmitter(testEmitter);
    }

    @Test
    void testInstrumentationOfSampleService() throws Exception {
        String className = "io.oi.core.agent.SampleService";
        String classAsResource = className.replace('.', '/') + ".class";

        // Read the original bytecode
        InputStream is = getClass().getClassLoader().getResourceAsStream(classAsResource);
        assertNotNull(is, "Could not find SampleService.class. Ensure it's compiled.");
        byte[] originalBytecode = is.readAllBytes();

        // Transform the bytecode
        byte[] transformedBytecode = transformer.transform(
                getClass().getClassLoader(), className, null, null, originalBytecode);

        // Verify that the bytecode was actually changed
        assertNotEquals(originalBytecode.length, transformedBytecode.length);

        // Load the transformed class using a custom class loader
        BytecodeClassLoader classLoader = new BytecodeClassLoader(transformedBytecode);
        Class<?> transformedClass = classLoader.loadClass(className);

        // Create an instance and invoke a method
        Object instance = transformedClass.getDeclaredConstructor().newInstance();
        Method greetMethod = transformedClass.getMethod("greet", String.class);
        Object result = greetMethod.invoke(instance, "World");

        // Assertions
        assertEquals("Hello, World", result);
        assertEquals(1, testEmitter.getReceivedTrees().size(), "Should have received one flow tree.");

        FlowTree tree = testEmitter.getReceivedTrees().get(0);
        assertNotNull(tree);
        assertEquals(className.replace('.', '/'), tree.getRootNode().getMethodDetails().className());
        assertEquals("greet", tree.getRootNode().getMethodDetails().methodName());
        assertNotNull(tree.getRootNode().getReturnValue());
        assertNull(tree.getRootNode().getException());
    }

    // A simple class loader to load a class from its byte array
    private static class BytecodeClassLoader extends ClassLoader {
        private final byte[] classBytes;
        public BytecodeClassLoader(byte[] classBytes) {
            this.classBytes = classBytes;
        }

        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
            if (name.equals("io.oi.core.agent.SampleService")) {
                return defineClass(name, classBytes, 0, classBytes.length);
            }
            return super.findClass(name);
        }
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