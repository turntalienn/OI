package io.oi.core.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class FlowTree {
    private final String traceId;
    private final FlowCallNode rootNode;
    private final long startNanos;
    private long endNanos;

    public FlowTree(String traceId, FlowCallNode rootNode) {
        this.traceId = traceId;
        this.rootNode = rootNode;
        this.startNanos = System.nanoTime();
    }

    @JsonCreator
    public FlowTree(@JsonProperty("traceId") String traceId,
                    @JsonProperty("rootNode") FlowCallNode rootNode,
                    @JsonProperty("startNanos") long startNanos,
                    @JsonProperty("endNanos") long endNanos) {
        this.traceId = traceId;
        this.rootNode = rootNode;
        this.startNanos = startNanos;
        this.endNanos = endNanos;
    }

    public void complete() {
        this.endNanos = System.nanoTime();
    }

    public String getTraceId() {
        return traceId;
    }

    public FlowCallNode getRootNode() {
        return rootNode;
    }

    public long getStartNanos() {
        return startNanos;
    }

    public long getEndNanos() {
        return endNanos;
    }

    public void setEndNanos(long endNanos) {
        this.endNanos = endNanos;
    }
} 