package io.oi.core.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public record ThreadInfo(
    long threadId,
    String threadName,
    boolean isVirtual
) {
    @JsonCreator
    public ThreadInfo(
            @JsonProperty("threadId") long threadId,
            @JsonProperty("threadName") String threadName,
            @JsonProperty("isVirtual") boolean isVirtual) {
        this.threadId = threadId;
        this.threadName = threadName;
        this.isVirtual = isVirtual;
    }

    public static ThreadInfo current() {
        Thread currentThread = Thread.currentThread();
        return new ThreadInfo(
            currentThread.threadId(),
            currentThread.getName(),
            currentThread.isVirtual()
        );
    }
} 