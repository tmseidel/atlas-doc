package com.mdeg.docsportal.dto;

public record WebhookConfigDto(
    String webhookUrl,
    String secret
) {}
