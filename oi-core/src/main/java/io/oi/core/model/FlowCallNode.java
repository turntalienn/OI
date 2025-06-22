package io.oi.core.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.oi.core.model.event.DbQueryEvent;

import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;

public class FlowCallNode {
    private final MethodDetails methodDetails;
    private final ExecutionDetails executionDetails;
    private final CodeAnalysis codeAnalysis;
    private final List<FlowCallNode> children = new CopyOnWriteArrayList<>();
    private final List<DbQueryEvent> dbEvents = new CopyOnWriteArrayList<>();
    private Object returnValue;
    private Throwable exception;

    public FlowCallNode(MethodDetails methodDetails, ExecutionDetails executionDetails, CodeAnalysis codeAnalysis) {
        this.methodDetails = methodDetails;
        this.executionDetails = executionDetails;
        this.codeAnalysis = codeAnalysis;
    }

    @JsonCreator
    public FlowCallNode(
            @JsonProperty("methodDetails") MethodDetails methodDetails,
            @JsonProperty("executionDetails") ExecutionDetails executionDetails,
            @JsonProperty("codeAnalysis") CodeAnalysis codeAnalysis,
            @JsonProperty("children") List<FlowCallNode> children,
            @JsonProperty("dbEvents") List<DbQueryEvent> dbEvents,
            @JsonProperty("returnValue") Object returnValue,
            @JsonProperty("exception") Throwable exception) {
        this.methodDetails = methodDetails;
        this.executionDetails = executionDetails;
        this.codeAnalysis = codeAnalysis;
        if (children != null) {
            this.children.addAll(children);
        }
        if (dbEvents != null) {
            this.dbEvents.addAll(dbEvents);
        }
        this.returnValue = returnValue;
        this.exception = exception;
    }


    public void addChild(FlowCallNode child) {
        children.add(child);
    }

    public void addDbEvent(DbQueryEvent event) {
        dbEvents.add(event);
    }

    public void complete(Object returnValue, Throwable exception) {
        this.returnValue = returnValue;
        this.exception = exception;
        this.executionDetails.setEndNanos(System.nanoTime());
    }

    // Getters

    public MethodDetails getMethodDetails() {
        return methodDetails;
    }

    public ExecutionDetails getExecutionDetails() {
        return executionDetails;
    }

    public CodeAnalysis getCodeAnalysis() {
        return codeAnalysis;
    }

    public List<FlowCallNode> getChildren() {
        return new ArrayList<>(children);
    }

    public List<DbQueryEvent> getDbEvents() {
        return new ArrayList<>(dbEvents);
    }

    public Object getReturnValue() {
        return returnValue;
    }

    public Throwable getException() {
        return exception;
    }
} 