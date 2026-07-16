package org.remus.docsportal.controller;

import org.remus.docsportal.dto.DocPageDto;
import org.remus.docsportal.dto.HealthStatusDto;
import org.remus.docsportal.model.entity.BuildRecord;
import org.remus.docsportal.repository.BuildRecordJpaRepository;
import org.remus.docsportal.repository.RepositoryConfigJpaRepository;
import org.remus.docsportal.service.BuildOrchestrator;
import org.remus.docsportal.service.DocScannerService;
import org.remus.docsportal.service.ProjectContext;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ApiHealthController {

    private final BuildOrchestrator buildOrchestrator;
    private final BuildRecordJpaRepository buildRecordRepo;
    private final RepositoryConfigJpaRepository repoDao;
    private final DocScannerService docScannerService;
    private final ProjectContext projectContext;

    @GetMapping("/health")
    public ResponseEntity<HealthStatusDto> health() {
        String projectId = projectContext.getProjectId();
        Optional<BuildRecord> latestBuild = buildRecordRepo.findTopByProjectIdOrderByStartedAtDesc(projectId);

        var repoStatuses = repoDao.findByProjectId(projectId).stream()
            .map(r -> new HealthStatusDto.RepoSyncStatusDto(
                r.getName(),
                r.getLastSyncStatus(),
                r.getLastSyncAt(),
                r.getLastSyncError()
            ))
            .toList();

        var status = new HealthStatusDto(
            buildOrchestrator.getCurrentBuildStatus(projectId),
            latestBuild.map(BuildRecord::getStartedAt).orElse(null),
            latestBuild.map(BuildRecord::getFinishedAt).orElse(null),
            repoStatuses.stream()
                .map(HealthStatusDto.RepoSyncStatusDto::lastSyncAt)
                .filter(Objects::nonNull)
                .max(java.util.Comparator.naturalOrder())
                .orElse(null),
            repoStatuses
        );

        return ResponseEntity.ok(status);
    }

    @GetMapping("/doc-pages")
    public ResponseEntity<List<DocPageDto>> docPages() {
        return ResponseEntity.ok(docScannerService.scanPages());
    }
}
