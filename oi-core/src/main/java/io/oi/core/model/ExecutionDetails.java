package io.oi.core.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ExecutionDetails {
    private final long startNanos;
    private long endNanos;
    private final ThreadInfo threadInfo;

    public ExecutionDetails(ThreadInfo threadInfo) {
        this.startNanos = System.nanoTime();
        this.threadInfo = threadInfo;
    }

    @JsonCreator
    public ExecutionDetails(
            @JsonProperty("startNanos") long startNanos,
            @JsonProperty("endNanos") long endNanos,
            @JsonProperty("threadInfo") ThreadInfo threadInfo) {
        this.startNanos = startNanos;
        this.endNanos = endNanos;
        this.threadInfo = threadInfo;
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

    public ThreadInfo getThreadInfo() {
        return threadInfo;
    }
} 