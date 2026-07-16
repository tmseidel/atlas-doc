package org.remus.docsportal.controller;

import org.remus.docsportal.dto.WebhookConfigDto;
import org.remus.docsportal.repository.RepositoryConfigJpaRepository;
import org.remus.docsportal.service.BuildOrchestrator;
import org.remus.docsportal.service.WebhookService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class WebhookController {

    private final WebhookService webhookService;
    private final BuildOrchestrator buildOrchestrator;
    private final RepositoryConfigJpaRepository repositoryConfigDao;

    @Value("${docs-portal.webhook.secret}")
    private String webhookSecret;

    @Value("${docs-portal.public-base-url:}")
    private String publicBaseUrl;

    /**
     * Receives Gitea push events. HMAC-verified, CSRF-disabled.
     */
    @PostMapping("/webhook/gitea")
    public ResponseEntity<?> handleGiteaWebhook(
            @RequestHeader(value = "X-Hub-Signature-256", required = false) String signature,
            @RequestBody String payload) {

        if (!webhookService.verifySignature(signature, payload, webhookSecret)) {
            log.warn("Webhook signature verification failed");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String repoName = webhookService.extractRepoName(payload);
        log.info("Webhook received for repo: {}", repoName);

        var matchingRepositories = repositoryConfigDao.findByName(repoName);
        if (matchingRepositories.isEmpty() && repoName.contains("/")) {
            matchingRepositories = repositoryConfigDao.findByName(repoName.substring(repoName.lastIndexOf('/') + 1));
        }
        if (matchingRepositories.isEmpty()) {
            log.warn("Ignoring webhook for unconfigured repository {}", repoName);
            return ResponseEntity.accepted().build();
        }
        matchingRepositories.forEach(repository -> buildOrchestrator.triggerBuild(
            "webhook", repoName, repository.getProject().getId()));

        return ResponseEntity.accepted().build();
    }

    @GetMapping("/webhook/config")
    public ResponseEntity<WebhookConfigDto> getWebhookConfig(HttpServletRequest request) {
        String baseUrl = publicBaseUrl == null || publicBaseUrl.isBlank()
            ? ServletUriComponentsBuilder.fromRequestUri(request)
                .replacePath(null)
                .replaceQuery(null)
                .build()
                .toUriString()
            : publicBaseUrl.replaceFirst("/+$", "");
        String webhookUrl = baseUrl + "/api/webhook/gitea";
        return ResponseEntity.ok(new WebhookConfigDto(webhookUrl, webhookSecret));
    }
}
