package io.oi.spring.adapter;

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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

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
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    void testLatestEndpoint() throws Exception {
        mockMvc.perform(get("/oi/trace/latest"))
                .andExpect(status().isNoContent()); // No flow in test context
    }

    // Health indicator test would require a @SpringBootTest and actuator context
} 