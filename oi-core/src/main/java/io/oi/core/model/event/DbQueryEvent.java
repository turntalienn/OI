package io.oi.core.model.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a single database query event captured during an execution.
 */
public record DbQueryEvent(
    String sql,
    long durationNanos,
    int rowCount
) {
    @JsonCreator
    public DbQueryEvent(
            @JsonProperty("sql") String sql,
            @JsonProperty("durationNanos") long durationNanos,
            @JsonProperty("rowCount") int rowCount) {
        this.sql = sql;
        this.durationNanos = durationNanos;
        this.rowCount = rowCount;
    }
} 