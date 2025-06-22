package io.oi.spring.adapter.web;

import io.oi.core.config.OiCoreProperties;
import io.oi.core.model.FlowCallNode;
import io.oi.core.model.FlowTree;
import io.oi.core.trace.Tracer;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Deque;
import java.util.UUID;

@RestController
@RequestMapping("/oi/trace")
public class OiTraceController {

    private final OiCoreProperties properties;

    public OiTraceController(OiCoreProperties properties) {
        this.properties = properties;
    }

    @GetMapping("/config")
    public ResponseEntity<OiCoreProperties> getConfig() {
        return ResponseEntity.ok(properties);
    }

    @GetMapping("/latest")
    public ResponseEntity<FlowTree> getLatestTrace() {
        Deque<FlowCallNode> stack = Tracer.getCurrentCallStack();

        if (stack.isEmpty()) {
            return ResponseEntity.noContent().build();
        }

        // The root of the trace is the last element in the deque
        FlowCallNode rootNode = stack.getLast();
        FlowTree liveTree = new FlowTree(UUID.randomUUID().toString(), rootNode);
        
        // Note: The endNanos will not be set, as the trace is still in progress.
        return ResponseEntity.ok(liveTree);
    }
} 