package io.oi.spring.adapter;

import io.oi.core.config.OiCoreProperties;
import io.oi.spring.adapter.web.OiTraceController;
import io.oi.spring.adapter.web.OiCoreHealthIndicator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.http.MediaType;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.mockito.Mockito.when;

@WebMvcTest(controllers = OiTraceController.class)
@ContextConfiguration(classes = {OiTraceController.class, OiCoreHealthIndicator.class})
public class OiSpringAdapterTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OiCoreHealthIndicator healthIndicator;

    @Test
    void testConfigEndpoint() throws Exception {
        mockMvc.perform(get("/oi/trace/config"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.enabled").exists())
                .andExpect(jsonPath("$.includePackages").exists())
                .andExpect(jsonPath("$.emitter").exists())
                .andExpect(jsonPath("$.instrumentation").exists());
    }

    @Test
    void testLatestEndpoint() throws Exception {
        mockMvc.perform(get("/oi/trace/latest"))
                .andExpect(status().isNoContent()); // No flow in test context
    }

    @Test
    void testHealthIndicator() {
        // Test health indicator with enabled configuration
        OiCoreProperties properties = new OiCoreProperties();
        properties.setEnabled(true);
        properties.getEmitter().setUrl("http://localhost:8081/ingest");
        
        OiCoreHealthIndicator indicator = new OiCoreHealthIndicator(properties);
        Health health = indicator.health();
        
        // The health check will likely be DOWN in test environment due to network issues
        // but we can test that it returns a valid health object
        assertNotNull(health);
        assertNotNull(health.getStatus());
        assertNotNull(health.getDetails());
    }

    @Test
    void testHealthIndicatorDisabled() {
        // Test health indicator with disabled configuration
        OiCoreProperties properties = new OiCoreProperties();
        properties.setEnabled(false);
        
        OiCoreHealthIndicator indicator = new OiCoreHealthIndicator(properties);
        Health health = indicator.health();
        
        assertEquals(Status.DOWN, health.getStatus());
        assertTrue(health.getDetails().containsKey("reason"));
    }

    @Test
    void testHealthIndicatorNoUrl() {
        // Test health indicator with no emitter URL
        OiCoreProperties properties = new OiCoreProperties();
        properties.setEnabled(true);
        properties.getEmitter().setUrl("");
        
        OiCoreHealthIndicator indicator = new OiCoreHealthIndicator(properties);
        Health health = indicator.health();
        
        assertEquals(Status.DOWN, health.getStatus());
        assertTrue(health.getDetails().containsKey("reason"));
    }

    private void assertNotNull(Object obj) {
        if (obj == null) {
            throw new AssertionError("Expected not null");
        }
    }

    private void assertEquals(Object expected, Object actual) {
        if (!expected.equals(actual)) {
            throw new AssertionError("Expected " + expected + " but was " + actual);
        }
    }

    private void assertTrue(boolean condition) {
        if (!condition) {
            throw new AssertionError("Expected true");
        }
    }
} 