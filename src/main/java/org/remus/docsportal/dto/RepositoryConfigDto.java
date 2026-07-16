package org.remus.docsportal.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.remus.docsportal.model.entity.SyncStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.List;

public record RepositoryConfigDto(
    String id,
    @NotBlank String name,
    @NotBlank String cloneUrl,
    String branch,
    @NotNull List<String> subdirectories,
    Boolean enabled,
    @JsonProperty(access = JsonProperty.Access.READ_ONLY) Boolean hasAuthToken,
    String authToken,
    Instant lastSyncAt,
    SyncStatus lastSyncStatus,
    String lastSyncError
) {
    public RepositoryConfigDto {
        if (branch == null || branch.isBlank()) {
            branch = "main";
        }
        if (enabled == null) {
            enabled = true;
        }
    }
}
