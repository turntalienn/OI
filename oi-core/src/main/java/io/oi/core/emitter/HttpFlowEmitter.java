package io.oi.core.emitter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.oi.core.config.OiCoreProperties;
import io.oi.core.model.FlowTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HttpFlowEmitter implements FlowEmitter {

    private static final Logger log = LoggerFactory.getLogger(HttpFlowEmitter.class);

    private final OiCoreProperties.EmitterProperties config;
    private final HttpClient httpClient;
    private final ExecutorService executorService;
    private final ObjectMapper objectMapper;

    public HttpFlowEmitter(OiCoreProperties.EmitterProperties config) {
        this.config = config;
        this.executorService = Executors.newVirtualThreadPerTaskExecutor();
        this.httpClient = HttpClient.newBuilder()
                .executor(this.executorService)
                .connectTimeout(Duration.ofMillis(config.getTimeoutMs()))
                .build();
        this.objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
    }

    @Override
    public void emit(FlowTree tree) {
        if (tree == null) {
            log.warn("Attempted to emit a null FlowTree.");
            return;
        }

        executorService.submit(() -> {
            try {
                String jsonBody = objectMapper.writeValueAsString(tree);
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(config.getUrl()))
                        .timeout(Duration.ofMillis(config.getTimeoutMs()))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                        .build();

                httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                        .thenAccept(response -> {
                            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                                log.debug("Successfully emitted FlowTree with traceId: {}", tree.getTraceId());
                            } else {
                                log.error("Failed to emit FlowTree. Status: {}, Body: {}", response.statusCode(), response.body());
                            }
                        })
                        .exceptionally(ex -> {
                            log.error("Exception while emitting FlowTree", ex);
                            return null;
                        });

            } catch (Exception e) {
                log.error("Failed to serialize or send FlowTree", e);
            }
        });
    }

    public void shutdown() {
        log.info("Shutting down HttpFlowEmitter's executor service.");
        executorService.shutdown();
    }
} 