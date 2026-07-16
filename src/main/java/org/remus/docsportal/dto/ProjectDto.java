package org.remus.docsportal.dto;

import jakarta.validation.constraints.NotBlank;

public record ProjectDto(
    String id,
    @NotBlank String name,
    String description
) {
}
