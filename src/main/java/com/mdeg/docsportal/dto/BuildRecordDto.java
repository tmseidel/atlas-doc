package com.mdeg.docsportal.dto;

import com.mdeg.docsportal.model.entity.BuildRecord;
import com.mdeg.docsportal.model.entity.BuildStatus;
import com.mdeg.docsportal.model.entity.BuildTrigger;

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
