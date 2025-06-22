package io.oi.core.emitter;

import io.oi.core.model.FlowTree;

/**
 * Defines the contract for emitting completed execution flow trees.
 * Implementations are responsible for processing or sending the FlowTree,
 * for example, by serializing it and sending it to a remote server.
 */
public interface FlowEmitter {
    /**
     * Emits a completed FlowTree. This method should be non-blocking
     * or execute its work on a background thread to avoid impacting the
     * performance of the instrumented application.
     *
     * @param tree The completed {@link FlowTree} to emit.
     */
    void emit(FlowTree tree);
} 