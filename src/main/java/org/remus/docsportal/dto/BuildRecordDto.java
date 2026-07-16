package org.remus.docsportal.dto;

import org.remus.docsportal.model.entity.BuildRecord;
import org.remus.docsportal.model.entity.BuildStatus;
import org.remus.docsportal.model.entity.BuildTrigger;

import java.time.Instant;

/** JSON representation of a build-history entry, intentionally excluding its JPA associations. */
public record BuildRecordDto(
    String id,
    BuildStatus status,
    Instant startedAt,
    Instant finishedAt,
    BuildTrigger trigger
) {
    public static BuildRecordDto from(BuildRecord record) {
        return new BuildRecordDto(
            record.getId(),
            record.getStatus(),
            record.getStartedAt(),
            record.getFinishedAt(),
            record.getTrigger()
        );
    }
}
