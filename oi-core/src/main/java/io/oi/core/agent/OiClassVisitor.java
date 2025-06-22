package io.oi.core.agent;

import io.oi.core.config.OiCoreProperties;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import java.util.ArrayList;
import java.util.List;

public class OiClassVisitor extends ClassVisitor {
    private final String className;
    private final OiCoreProperties properties;
    private boolean isController, isService, isRepository;
    private boolean isJdbcStatement;
    private final List<String> classAnnotations = new ArrayList<>();

    public OiClassVisitor(ClassVisitor classVisitor, String className, OiCoreProperties properties) {
        super(Opcodes.ASM9, classVisitor);
        this.className = className;
        this.properties = properties;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        super.visit(version, access, name, signature, superName, interfaces);
        try {
            Class<?> clazz = Class.forName(name.replace('/', '.'), false, getClass().getClassLoader());
            if (java.sql.Statement.class.isAssignableFrom(clazz)) {
                this.isJdbcStatement = true;
            }
        } catch (Throwable e) {
            // Ignore
        }
    }

    @Override
    public org.objectweb.asm.AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
        classAnnotations.add(descriptor);
        String annotationName = descriptor.substring(1, descriptor.length() - 1).replace('/', '.');
        if ("org.springframework.stereotype.Controller".equals(annotationName) || "org.springframework.web.bind.annotation.RestController".equals(annotationName)) {
            isController = true;
        }
        if ("org.springframework.stereotype.Service".equals(annotationName)) {
            isService = true;
        }
        if ("org.springframework.stereotype.Repository".equals(annotationName)) {
            isRepository = true;
        }
        return super.visitAnnotation(descriptor, visible);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
        if (mv == null || !shouldInstrumentMethod(name)) {
            return mv;
        }

        return new OiMethodAdapter(mv, access, name, desc, className, properties);
    }

    private boolean shouldInstrumentMethod(String methodName) {
        // Don't instrument constructors or static initializers
        if (methodName.equals("<init>") || methodName.equals("<clinit>")) {
            return false;
        }

        // Instrument JDBC execute methods
        if (isJdbcStatement) {
            return methodName.startsWith("execute") || methodName.equals("addBatch");
        }

        // Check for specific annotations based on config
        if (properties.getInstrumentation().isController() && isController) return true;
        if (properties.getInstrumentation().isService() && isService) return true;
        if (properties.getInstrumentation().isRepository() && isRepository) return true;

        // Special case for Thread.start()
        if (properties.getInstrumentation().isThreads() && "java/lang/Thread".equals(className.replace('/','.')) && "start".equals(methodName)) {
            return true;
        }

        // Check for included packages (already pre-filtered in transformer, but good for safety)
        for (String pkg : properties.getIncludePackages()) {
            if (className.replace('/','.').startsWith(pkg)) {
                return true;
            }
        }
        
        return false;
    }
} 