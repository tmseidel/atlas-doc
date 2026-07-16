package org.remus.docsportal.dto;

public record WebhookConfigDto(
    String webhookUrl,
    String secret
) {}
