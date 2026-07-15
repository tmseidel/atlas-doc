package com.mdeg.docsportal.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WebhookServiceTest {

    private WebhookService webhookService;

    @BeforeEach
    void setUp() {
        webhookService = new WebhookService();
    }

    @Test
    void extractRepoName_fromFullName() {
        String payload = """
            {
              "repository": {
                "full_name": "my-org/my-repo",
                "name": "my-repo"
              }
            }
            """;

        String name = webhookService.extractRepoName(payload);

        assertThat(name).isEqualTo("my-org/my-repo");
    }

    @Test
    void extractRepoName_fromNameOnly() {
        String payload = """
            {
              "repository": {
                "name": "simple-repo"
              }
            }
            """;

        String name = webhookService.extractRepoName(payload);

        assertThat(name).isEqualTo("simple-repo");
    }

    @Test
    void extractRepoName_unknownFallback() {
        String payload = "{}";

        String name = webhookService.extractRepoName(payload);

        assertThat(name).isEqualTo("unknown");
    }

    @Test
    void extractRepoName_invalidJson() {
        String payload = "not json at all";

        String name = webhookService.extractRepoName(payload);

        assertThat(name).isEqualTo("unknown");
    }
}
