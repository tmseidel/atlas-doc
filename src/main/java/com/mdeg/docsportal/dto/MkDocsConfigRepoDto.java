package com.mdeg.docsportal.dto;

import jakarta.validation.constraints.NotBlank;

public record MkDocsConfigRepoDto(
    String id,
    @NotBlank String cloneUrl,
    String branch,
    String authToken
) {}
