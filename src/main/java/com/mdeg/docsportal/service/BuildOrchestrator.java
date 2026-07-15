package com.mdeg.docsportal.service;

import com.mdeg.docsportal.model.entity.BuildRecord;
import com.mdeg.docsportal.model.entity.BuildStatus;
import com.mdeg.docsportal.repository.BuildRecordJpaRepository;
import com.mdeg.docsportal.repository.ProjectJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class BuildOrchestrator {

    private final GitSyncService gitSyncService;
    private final MkDocsBuildService mkDocsBuildService;
    private final BuildRecordJpaRepository buildRecordRepo;
    private final ProjectJpaRepository projectRepo;
    private final Set<String> runningProjects = ConcurrentHashMap.newKeySet();

    @Scheduled(fixedRateString = "${docs-portal.poll-interval-ms:300000}")
    public void periodicSyncAndBuild() {
        projectRepo.findAll().forEach(project -> runSyncAndBuildIfChanged("polling", project.getId()));
    }

    @Async
    public CompletableFuture<Void> triggerBuild(String triggerType, String repoName, String projectId) {
        log.info("Build triggered: {} for project {} repository {}", triggerType, projectId, repoName);
        runSyncAndBuildIfChanged(triggerType, projectId);
        return CompletableFuture.completedFuture(null);
    }

    public void runSyncAndBuildIfChanged(String triggerType, String projectId) {
        if (!runningProjects.add(projectId)) {
            log.debug("Build already running for project {}, skipping", projectId);
            return;
        }
        try {
            boolean changed = gitSyncService.syncAllRepositories(projectId);
            boolean configChanged = gitSyncService.syncMkDocsConfigRepository(projectId);
            if (changed || configChanged || "manual".equalsIgnoreCase(triggerType)) {
                mkDocsBuildService.runBuild(triggerType, projectId);
            }
        } finally {
            runningProjects.remove(projectId);
        }
    }

    public BuildStatus getCurrentBuildStatus(String projectId) {
        if (runningProjects.contains(projectId)) return BuildStatus.RUNNING;
        return buildRecordRepo.findTopByProjectIdOrderByStartedAtDesc(projectId)
            .map(BuildRecord::getStatus)
            .orElse(BuildStatus.IDLE);
    }

    public boolean isBuildRunning(String projectId) {
        return runningProjects.contains(projectId);
    }
}
