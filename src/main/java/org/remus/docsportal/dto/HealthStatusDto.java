package org.remus.docsportal.dto;

import org.remus.docsportal.model.entity.BuildStatus;
import org.remus.docsportal.model.entity.SyncStatus;

import java.time.Instant;
import java.util.List;

public record HealthStatusDto(
    BuildStatus buildStatus,
    Instant lastBuildStartedAt,
    Instant lastBuildFinishedAt,
    Instant lastSyncAt,
    List<RepoSyncStatusDto> repoStatuses
) {
    public record RepoSyncStatusDto(
        String name,
        SyncStatus lastSyncStatus,
        Instant lastSyncAt,
        String lastSyncError
    ) {}
}
