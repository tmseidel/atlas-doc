package com.mdeg.docsportal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

/**
 * Integration smoke test — verifies the application context starts,
 * Flyway migrations run, and basic endpoints are registered.
 */
@SpringBootTest
class DocsPortalIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc() {
        return webAppContextSetup(context).build();
    }

    @Test
    void contextLoads() {
        // If this test runs, the ApplicationContext loaded successfully
    }

    @Test
    void loginPage_isAccessible() throws Exception {
        mockMvc().perform(get("/login"))
            .andExpect(status().isOk());
    }

    @Test
    void healthEndpoint_responds() throws Exception {
        // /api/health is protected by Spring Security
        // Returns 200 because MockMvc bypasses the security filter in this configuration
        // This is a known limitation — security integration tests should use a real HTTP client
        mockMvc().perform(get("/api/health"))
            .andExpect(status().isOk());
    }

    @Test
    void docPagesEndpoint_responds() throws Exception {
        mockMvc().perform(get("/api/doc-pages"))
            .andExpect(status().isOk());
    }

    @Test
    void webhookConfigEndpoint_responds() throws Exception {
        mockMvc().perform(get("/api/webhook/config"))
            .andExpect(status().isOk());
    }
}
