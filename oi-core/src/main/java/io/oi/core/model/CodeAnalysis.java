package io.oi.core.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

public record CodeAnalysis(
    int instructionCount,
    int maxLocalVariables,
    List<String> conditionalBranches
) {
    @JsonCreator
    public CodeAnalysis(
            @JsonProperty("instructionCount") int instructionCount,
            @JsonProperty("maxLocalVariables") int maxLocalVariables,
            @JsonProperty("conditionalBranches") List<String> conditionalBranches) {
        this.instructionCount = instructionCount;
        this.maxLocalVariables = maxLocalVariables;
        this.conditionalBranches = conditionalBranches;
    }
} 