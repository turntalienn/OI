package io.oi.core.agent;

import io.oi.core.config.OiCoreProperties;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.AdviceAdapter;
import org.objectweb.asm.commons.LocalVariablesSorter;

public class OiMethodAdapter extends AdviceAdapter {
    private final String methodName;
    private final String className;
    private final String methodDesc;
    private final OiCoreProperties properties;
    private int instructionCount = 0;
    private int maxLocals = 0;
    private final boolean isJdbcStatement;
    private int startTimeVar = -1;

    protected OiMethodAdapter(MethodVisitor methodVisitor, int access, String name, String desc, String className, OiCoreProperties properties) {
        super(ASM9, methodVisitor, access, name, desc);
        this.methodName = name;
        this.methodDesc = desc;
        this.className = className;
        this.properties = properties;
        this.isJdbcStatement = isJdbcStatement(className);
        this.maxLocals = (Opcodes.ACC_STATIC & access) != 0 ? 0 : 1; // `this` pointer
        for (Type t : Type.getArgumentTypes(desc)) {
            maxLocals += t.getSize();
        }
    }

    private boolean isJdbcStatement(String className) {
        try {
            Class<?> clazz = Class.forName(className.replace('/', '.'), false, getClass().getClassLoader());
            return java.sql.Statement.class.isAssignableFrom(clazz);
        } catch (Throwable e) {
            return false;
        }
    }

    @Override
    public void visitInsn(int opcode) {
        instructionCount++;
        super.visitInsn(opcode);
    }

    @Override
    public void visitVarInsn(int opcode, int var) {
        instructionCount++;
        super.visitVarInsn(opcode, var);
    }
    
    @Override
    public void visitMaxs(int maxStack, int maxLocals) {
        this.maxLocals = Math.max(this.maxLocals, maxLocals);
        super.visitMaxs(maxStack, maxLocals);
    }

    @Override
    protected void onMethodEnter() {
        if (isJdbcStatement && methodName.startsWith("execute")) {
            startTimeVar = newLocal(Type.LONG_TYPE);
            mv.visitMethodInsn(INVOKESTATIC, "java/lang/System", "nanoTime", "()J", false);
            mv.visitVarInsn(LSTORE, startTimeVar);
        } else {
            // Pass class name, method name, and method descriptor to the tracer
            mv.visitLdcInsn(className);
            mv.visitLdcInsn(methodName);
            mv.visitLdcInsn(methodDesc);

            // Pass bytecode analysis results
            mv.visitIntInsn(SIPUSH, instructionCount);
            mv.visitIntInsn(SIPUSH, maxLocals);

            // Create an array of objects to hold the method parameters
            Type[] argumentTypes = Type.getArgumentTypes(methodDesc);
            mv.visitIntInsn(BIPUSH, argumentTypes.length);
            mv.visitTypeInsn(ANEWARRAY, "java/lang/Object");

            // Load each parameter, box it if primitive, and store it in the array
            for (int i = 0; i < argumentTypes.length; i++) {
                mv.visitInsn(DUP);
                mv.visitIntInsn(BIPUSH, i);
                loadArg(i);
                box(argumentTypes[i]);
                mv.visitInsn(AASTORE);
            }

            // Call the static startTrace method
            mv.visitMethodInsn(INVOKESTATIC, "io/oi/core/trace/Tracer", "startTrace",
                    "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;II[Ljava/lang/Object;)V", false);
        }
    }

    @Override
    protected void onMethodExit(int opcode) {
        if (isJdbcStatement && methodName.startsWith("execute")) {
            if (opcode != ATHROW) {
                // For a successful execution, calculate duration and record the event
                mv.visitMethodInsn(INVOKESTATIC, "java/lang/System", "nanoTime", "()J", false);
                mv.visitVarInsn(LLOAD, startTimeVar);
                mv.visitInsn(LSUB);
                int durationVar = newLocal(Type.LONG_TYPE);
                mv.visitVarInsn(LSTORE, durationVar);

                // Load SQL from the first argument
                loadArg(0); 
                
                // Load duration
                mv.visitVarInsn(LLOAD, durationVar);

                // Get row count from return value (if applicable)
                if (opcode == IRETURN) { // executeUpdate returns an int
                    dup(); // duplicate the return value (rowCount)
                } else {
                    mv.visitInsn(ICONST_M1); // -1 for unknown/not applicable row count
                }
                
                mv.visitMethodInsn(INVOKESTATIC, "io/oi/core/trace/Tracer", "recordDbQuery", "(Ljava/lang/String;JI)V", false);
            }
        } else {
            // Original tracing logic for non-DB methods
            if (opcode != ATHROW) {
                // If it's a normal return, load the return value onto the stack
                if (opcode == RETURN) { // void return
                    mv.visitInsn(ACONST_NULL);
                } else {
                    // If there is a return value, duplicate it and box if necessary
                    if (opcode == LRETURN || opcode == DRETURN) {
                        dup2();
                    } else {
                        dup();
                    }
                    box(Type.getReturnType(methodDesc));
                }
                mv.visitInsn(ACONST_NULL); // No exception
            } else {
                // Exception is already on the stack
                mv.visitInsn(ACONST_NULL); // No return value
                mv.visitInsn(SWAP); // Swap to get exception on top
            }

            mv.visitMethodInsn(INVOKESTATIC, "io/oi/core/trace/Tracer", "endTrace",
                    "(Ljava/lang/Object;Ljava/lang/Throwable;)V", false);
        }
    }
} 