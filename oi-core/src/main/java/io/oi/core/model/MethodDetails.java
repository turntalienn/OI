package io.oi.core.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

public record MethodDetails(
    String className,
    String methodName,
    String methodSignature,
    Map<String, Object> parameters
) {
    @JsonCreator
    public MethodDetails(
            @JsonProperty("className") String className,
            @JsonProperty("methodName") String methodName,
            @JsonProperty("methodSignature") String methodSignature,
            @JsonProperty("parameters") Map<String, Object> parameters) {
        this.className = className;
        this.methodName = methodName;
        this.methodSignature = methodSignature;
        this.parameters = parameters;
    }
} 