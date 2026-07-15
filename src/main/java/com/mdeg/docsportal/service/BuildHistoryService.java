package com.mdeg.docsportal.service;

import com.mdeg.docsportal.model.entity.BuildRecord;
import com.mdeg.docsportal.repository.BuildRecordJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

/** Keeps build history bounded while preserving the most recent records. */
@Service
@RequiredArgsConstructor
@Slf4j
public class BuildHistoryService {

    private static final int MAX_RECORDS = 20;

    private final BuildRecordJpaRepository buildRecordRepo;

    @Transactional
    public void retainLatestTwenty(String projectId) {
        List<BuildRecord> records = buildRecordRepo.findByProjectId(projectId).stream()
            .sorted(Comparator.comparing(BuildRecord::getStartedAt).reversed())
            .toList();

        if (records.size() <= MAX_RECORDS) {
            return;
        }

        List<BuildRecord> obsoleteRecords = records.subList(MAX_RECORDS, records.size());
        buildRecordRepo.deleteAllInBatch(obsoleteRecords);
        log.info("Removed {} obsolete build history records for project {}", obsoleteRecords.size(), projectId);
    }
}
