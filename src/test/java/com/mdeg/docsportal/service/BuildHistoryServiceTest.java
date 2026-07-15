package com.mdeg.docsportal.service;

import com.mdeg.docsportal.model.entity.BuildRecord;
import com.mdeg.docsportal.repository.BuildRecordJpaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BuildHistoryServiceTest {

    @Mock
    private BuildRecordJpaRepository buildRecordRepo;

    @InjectMocks
    private BuildHistoryService buildHistoryService;

    @Test
    void retainLatestTwenty_deletesOnlyOlderRecords() {
        List<BuildRecord> records = IntStream.range(0, 22)
            .mapToObj(index -> BuildRecord.builder()
                .id("build-" + index)
                .startedAt(Instant.parse("2026-01-01T00:00:00Z").plusSeconds(index))
                .build())
            .toList();
        when(buildRecordRepo.findByProjectId("project-1")).thenReturn(records);

        buildHistoryService.retainLatestTwenty("project-1");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<BuildRecord>> deleted = ArgumentCaptor.forClass(List.class);
        verify(buildRecordRepo).deleteAllInBatch(deleted.capture());
        assertThat(deleted.getValue()).extracting(BuildRecord::getId)
            .containsExactly("build-1", "build-0");
    }
}
