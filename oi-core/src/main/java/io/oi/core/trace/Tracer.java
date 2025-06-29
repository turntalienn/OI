package io.oi.core.trace;

import io.oi.core.analysis.AnalysisService;
import io.oi.core.emitter.FlowEmitter;
import io.oi.core.model.*;
import io.oi.core.model.event.DbQueryEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The central static class for managing the lifecycle of traces.
 * <p>
 * This class uses a {@link ThreadLocal} to maintain a call stack for each thread,
 * allowing it to build a hierarchical {@link FlowTree} that represents a single
 * execution flow (e.g., an HTTP request).
 * <p>
 * The static methods {@code startTrace} and {@code endTrace} are designed to be called
 * from bytecode injected by the {@link io.oi.core.agent.OiMethodAdapter}.
 */
public final class Tracer {

    private static final Logger log = LoggerFactory.getLogger(Tracer.class);
    private static final ThreadLocal<Deque<FlowCallNode>> callStack = ThreadLocal.withInitial(ArrayDeque::new);
    private static FlowEmitter emitter;
    private static final Map<String, Method> methodCache = new ConcurrentHashMap<>();

    private Tracer() {
        // Static utility class
    }

    /**
     * Configures the emitter that will be used to send completed FlowTrees.
     * This must be called once during agent initialization.
     *
     * @param flowEmitter The emitter instance.
     */
    public static void setEmitter(FlowEmitter flowEmitter) {
        emitter = flowEmitter;
    }

    /**
     * Gets the current call stack for the executing thread.
     * This is intended for diagnostic endpoints and should be used with caution.
     *
     * @return A deque of the current call nodes, or an empty deque if none.
     */
    public static Deque<FlowCallNode> getCurrentCallStack() {
        return callStack.get();
    }

    private static void startTraceInternal(FlowCallNode node) {
        if (emitter == null) {
            log.trace("No emitter configured, skipping trace.");
            return;
        }

        Deque<FlowCallNode> stack = callStack.get();
        if (!stack.isEmpty()) {
            FlowCallNode parent = stack.peek();
            if (parent != null) {
                parent.addChild(node);
                node.setCallDepth(parent.getCallDepth() + 1);
            }
        } else {
            node.setCallDepth(0);
        }
        stack.push(node);
    }

    private static void endTraceInternal(Object returnValue, Throwable exception) {
        Deque<FlowCallNode> stack = callStack.get();
        if (stack.isEmpty()) {
            // This can happen if instrumentation is misconfigured or applied partially.
            return;
        }

        FlowCallNode node = stack.pop();
        node.complete(returnValue, exception);

        if (stack.isEmpty()) {
            // This was the root node, so the trace for this thread is complete.
            FlowTree tree = new FlowTree(UUID.randomUUID().toString(), node);
            tree.complete();
            emitter.emit(tree);
            callStack.remove(); // Clean up ThreadLocal
        }
    }

    /**
     * Marks the entry of an instrumented method.
     * <p>
     * This method is called from injected bytecode. It constructs a {@link FlowCallNode}
     * with all the method's metadata and pushes it onto the current thread's call stack.
     *
     * @param className        The FQDN name of the class.
     * @param methodName       The name of the method.
     * @param methodDesc       The method's signature in JVM descriptor format.
     * @param instructionCount The number of bytecode instructions in the method.
     * @param maxLocals        The maximum number of local variables used by the method.
     * @param parameters       An array of the method's arguments.
     */
    public static void startTrace(String className, String methodName, String methodDesc,
                                  int instructionCount, int maxLocals, Object[] parameters) {
        try {
            Method method = resolveMethod(className, methodName, methodDesc);

            Map<String, Object> parameterMap = (method != null)
                ? AnalysisService.getParameterMap(method, parameters)
                : Map.of();

            List<String> conditionalBranches = AnalysisService.getConditionalBranches(className, methodName, methodDesc);

            CodeAnalysis codeAnalysis = new CodeAnalysis(instructionCount, maxLocals, conditionalBranches);
            MethodDetails methodDetails = new MethodDetails(className, methodName, methodDesc, parameterMap);
            ExecutionDetails executionDetails = new ExecutionDetails(ThreadInfo.current());

            FlowCallNode node = new FlowCallNode(methodDetails, executionDetails, codeAnalysis);
            startTraceInternal(node);
        } catch (Exception e) {
            log.warn("Error starting trace in {}.{}: {}", className, methodName, e.getMessage());
        }
    }

    /**
     * Marks the exit of an instrumented method.
     * <p>
     * This method is called from injected bytecode. It pops the current {@link FlowCallNode}
     * from the stack, completes it with the return value or exception, and if the stack
     * becomes empty, emits the entire {@link FlowTree}.
     *
     * @param returnValue The value returned by the method, or null if void or an exception was thrown.
     * @param exception   The exception thrown by the method, or null if it completed normally.
     */
    public static void endTrace(Object returnValue, Throwable exception) {
        try {
            endTraceInternal(returnValue, exception);
        } catch (Exception e) {
            log.warn("Error ending trace: {}", e.getMessage());
        }
    }

    /**
     * Records a database query event that occurred during an instrumented method's execution.
     * <p>
     * This method is called from injected bytecode. It finds the current active
     * {@link FlowCallNode} and attaches a {@link DbQueryEvent} to it.
     *
     * @param sql           The SQL query that was executed.
     * @param durationNanos The time taken to execute the query.
     * @param rowCount      The number of rows returned or affected.
     */
    public static void recordDbQuery(String sql, long durationNanos, int rowCount) {
        Deque<FlowCallNode> stack = callStack.get();
        if (stack.isEmpty()) {
            log.trace("DB query recorded but no active trace call stack found. Skipping.");
            return;
        }
        FlowCallNode currentNode = stack.peek();
        if (currentNode != null) {
            currentNode.addDbEvent(new DbQueryEvent(sql, durationNanos, rowCount));
        }
    }

    /**
     * Records that a branch (e.g., if/else) was taken in the current method.
     */
    public static void recordBranchTaken(String branch) {
        Deque<FlowCallNode> stack = callStack.get();
        if (!stack.isEmpty()) {
            stack.peek().addBranchTaken(branch);
        }
    }

    /**
     * Records that a loop (e.g., for/while) was entered in the current method.
     */
    public static void recordLoopEntered(String loop) {
        Deque<FlowCallNode> stack = callStack.get();
        if (!stack.isEmpty()) {
            stack.peek().addLoopEntered(loop);
        }
    }

    /**
     * Sets the call depth for the current node.
     */
    public static void setCallDepth(int depth) {
        Deque<FlowCallNode> stack = callStack.get();
        if (!stack.isEmpty()) {
            stack.peek().setCallDepth(depth);
        }
    }

    private static Method resolveMethod(String className, String methodName, String methodDesc) {
        String key = className + "#" + methodName + "#" + methodDesc;
        return methodCache.computeIfAbsent(key, k -> {
            try {
                Class<?> clazz = Class.forName(className.replace('/', '.'));
                // This is a simplification. A robust implementation needs to parse methodDesc to find param types.
                return Arrays.stream(clazz.getDeclaredMethods())
                        .filter(m -> m.getName().equals(methodName))
                        .findFirst()
                        .orElse(null);
            } catch (ClassNotFoundException e) {
                log.trace("Could not find class for reflection: {}", className);
                return null;
            }
        });
    }
} 