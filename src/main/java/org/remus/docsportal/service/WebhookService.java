package org.remus.docsportal.service;

import org.remus.docsportal.util.GiteaHmacVerifier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Verifies Gitea webhook HMAC signatures and extracts repo names from payloads.
 */
@Service
@Slf4j
public class WebhookService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Verifies the HMAC signature from a Gitea webhook.
     */
    public boolean verifySignature(String signature, String payload, String secret) {
        return GiteaHmacVerifier.verify(signature, payload, secret);
    }

    /**
     * Extracts the repository name from a Gitea push webhook payload.
     */
    public String extractRepoName(String payload) {
        try {
            JsonNode root = objectMapper.readTree(payload);
            // Gitea payloads have a "repository" object with "full_name"
            JsonNode repo = root.get("repository");
            if (repo != null) {
                JsonNode fullName = repo.get("full_name");
                if (fullName != null && !fullName.asString().isEmpty()) {
                    return fullName.asString();
                }
                // Fallback: try "repository.name"
                JsonNode name = repo.get("name");
                if (name != null && !name.asString().isEmpty()) {
                    return name.asString();
                }
            }
            return "unknown";
        } catch (Exception e) {
            log.warn("Failed to parse webhook payload: {}", e.getMessage());
            return "unknown";
        }
    }
}
