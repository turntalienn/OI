package io.oi.core.agent;

import io.oi.core.config.OiCoreProperties;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.List;

public class OiClassFileTransformer implements ClassFileTransformer {

    private static final Logger log = LoggerFactory.getLogger(OiClassFileTransformer.class);
    private final OiCoreProperties properties;
    private final List<String> includePackages;

    public OiClassFileTransformer(OiCoreProperties properties) {
        this.properties = properties;
        this.includePackages = properties.getIncludePackages().stream().map(p -> p.replace('.', '/')).toList();
    }

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                            ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {

        if (className == null || !shouldTransform(className)) {
            return classfileBuffer; // No transformation
        }

        try {
            log.trace("Transforming class: {}", className);
            ClassReader cr = new ClassReader(classfileBuffer);
            ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
            OiClassVisitor cv = new OiClassVisitor(cw, className, properties);
            cr.accept(cv, ClassReader.EXPAND_FRAMES);
            return cw.toByteArray();
        } catch (Exception e) {
            log.error("Error transforming class " + className, e);
            return classfileBuffer; // Return original bytecode on error
        }
    }

    private boolean shouldTransform(String className) {
        // Avoid instrumenting self, JDK, and other common libraries to prevent recursion and performance issues.
        if (className.startsWith("io/oi/core/") ||
            className.startsWith("java/") ||
            className.startsWith("javax/") ||
            className.startsWith("sun/") ||
            className.startsWith("com/sun/") ||
            className.startsWith("jdk/") ||
            className.startsWith("org/slf4j/") ||
            className.startsWith("com/fasterxml/jackson/")) {
            return false;
        }

        // Suppress framework noise unless explicitly configured
        if (!properties.getInstrumentation().isFramework()) {
            if (className.startsWith("org/springframework/") ||
                className.startsWith("org/apache/catalina/") ||
                className.startsWith("org/apache/tomcat/")) {
                return false;
            }
        }

        // Instrument java.lang.Thread specifically for thread start interception
        if (properties.getInstrumentation().isThreads() && "java/lang/Thread".equals(className)) {
            return true;
        }

        // Instrument JDBC statements, but only if not framework noise
        if (isJdbcStatement(className) && (properties.getInstrumentation().isFramework() || !className.startsWith("org/springframework/"))) {
            return true;
        }

        // Check against user-configured packages
        for (String pkg : includePackages) {
            if (className.replace('/', '.').startsWith(pkg)) {
                return true;
            }
        }

        return false;
    }

    private boolean isJdbcStatement(String className) {
        if (className == null) {
            return false;
        }
        try {
            Class<?> clazz = Class.forName(className.replace('/', '.'), false, ClassLoader.getSystemClassLoader());
            if (java.sql.Statement.class.isAssignableFrom(clazz)) {
                log.trace("Identified JDBC Statement for transformation: {}", className);
                return true;
            }
        } catch (Throwable e) {
            // Ignore errors like ClassNotFoundException, NoClassDefFoundError, etc.
        }
        return false;
    }
} 