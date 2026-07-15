package com.mdeg.docsportal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

/**
 * Integration tests for role-based access control.
 * Verifies that Admin and Viewer roles have the correct access levels.
 */
@SpringBootTest
class SecurityIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = webAppContextSetup(context)
            .apply(SecurityMockMvcConfigurers.springSecurity())
            .build();
    }

    // ─── Public endpoints ────────────────────────────────────

    @Test
    @DisplayName("Login page is accessible without authentication")
    void loginPage_isPublic() throws Exception {
        mockMvc.perform(get("/login"))
            .andExpect(status().isOk());
    }

    // ─── Wrong password rejected ────────────────────────────

    @Test
    @DisplayName("Wrong password is rejected")
    void wrongPassword_isRejected() throws Exception {
        mockMvc.perform(formLogin()
                .user("viewer")
                .password("wrong"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/login?error"));
    }

    @Test
    @DisplayName("Webhook endpoint is reachable without a browser session")
    void webhookEndpoint_isUnauthenticatedButHmacProtected() throws Exception {
        mockMvc.perform(post("/api/webhook/gitea")
                .contentType("application/json")
                .content("{}"))
            .andExpect(status().isUnauthorized());
    }

    // ─── Viewer: allowed ─────────────────────────────────────

    @Nested
    @WithMockUser(roles = {"VIEWER"})
    @DisplayName("As a Viewer")
    class ViewerTests {

        @Test
        @DisplayName("can access /docs")
        void canAccessDocs() throws Exception {
            mockMvc.perform(get("/docs"))
                .andExpect(status().isOk());
        }

        @Test
        @DisplayName("can access /api/health")
        void canAccessHealth() throws Exception {
            mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk());
        }

        @Test
        @DisplayName("can access /api/build/status")
        void canAccessBuildStatus() throws Exception {
            mockMvc.perform(get("/api/build/status"))
                .andExpect(status().isOk());
        }

        @Test
        @DisplayName("is blocked from /admin/**")
        void blockedFromAdmin() throws Exception {
            mockMvc.perform(get("/admin/repos"))
                .andExpect(status().isForbidden());
            mockMvc.perform(get("/admin/build"))
                .andExpect(status().isForbidden());
            mockMvc.perform(get("/admin/mkdocs-config"))
                .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("is blocked from /api/repositories/**")
        void blockedFromRepoApi() throws Exception {
            mockMvc.perform(get("/api/repositories"))
                .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("is blocked from /api/mkdocs-config/**")
        void blockedFromMkDocsConfigApi() throws Exception {
            mockMvc.perform(get("/api/mkdocs-config"))
                .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("is blocked from /api/build/trigger")
        void blockedFromBuildTrigger() throws Exception {
            mockMvc.perform(post("/api/build/trigger").with(csrf()))
                .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("is blocked from /api/webhook/config")
        void blockedFromWebhookConfig() throws Exception {
            mockMvc.perform(get("/api/webhook/config"))
                .andExpect(status().isForbidden());
        }
    }

    // ─── Admin: full access ─────────────────────────────────

    @Nested
    @WithMockUser(roles = {"ADMIN", "VIEWER"})
    @DisplayName("As an Admin")
    class AdminTests {

        @Test
        @DisplayName("can access /admin/**")
        void canAccessAdminPages() throws Exception {
            mockMvc.perform(get("/admin/repos"))
                .andExpect(status().isOk());
            mockMvc.perform(get("/admin/build"))
                .andExpect(status().isOk());
            mockMvc.perform(get("/admin/mkdocs-config"))
                .andExpect(status().isOk());
        }

        @Test
        @DisplayName("can access viewer endpoints too")
        void canAccessViewerEndpoints() throws Exception {
            mockMvc.perform(get("/docs"))
                .andExpect(status().isOk());
            mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk());
        }
    }
}
