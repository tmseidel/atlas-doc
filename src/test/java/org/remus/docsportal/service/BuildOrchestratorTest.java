package org.remus.docsportal.service;

import org.remus.docsportal.model.entity.BuildStatus;
import org.remus.docsportal.repository.BuildRecordJpaRepository;
import org.remus.docsportal.repository.ProjectJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BuildOrchestratorTest {

    private static final String PROJECT_ID = "project-1";

    @Mock private GitSyncService gitSyncService;
    @Mock private MkDocsBuildService mkDocsBuildService;
    @Mock private BuildRecordJpaRepository buildRecordRepo;
    @Mock private ProjectJpaRepository projectRepo;
    private BuildOrchestrator buildOrchestrator;

    @BeforeEach
    void setUp() {
        buildOrchestrator = new BuildOrchestrator(gitSyncService, mkDocsBuildService, buildRecordRepo, projectRepo);
    }

    @Test
    void noChangesDoNotBuildProject() {
        when(gitSyncService.syncAllRepositories(PROJECT_ID)).thenReturn(false);
        when(gitSyncService.syncMkDocsConfigRepository(PROJECT_ID)).thenReturn(false);

        buildOrchestrator.runSyncAndBuildIfChanged("polling", PROJECT_ID);

        verify(mkDocsBuildService, never()).runBuild(anyString(), anyString());
    }

    @Test
    void changesBuildOnlyTheSelectedProject() {
        when(gitSyncService.syncAllRepositories(PROJECT_ID)).thenReturn(true);

        buildOrchestrator.runSyncAndBuildIfChanged("polling", PROJECT_ID);

        verify(mkDocsBuildService).runBuild("polling", PROJECT_ID);
    }

    @Test
    void manualBuildRunsWithoutChanges() {
        when(gitSyncService.syncAllRepositories(PROJECT_ID)).thenReturn(false);
        when(gitSyncService.syncMkDocsConfigRepository(PROJECT_ID)).thenReturn(false);

        buildOrchestrator.runSyncAndBuildIfChanged("manual", PROJECT_ID);

        verify(mkDocsBuildService).runBuild("manual", PROJECT_ID);
    }

    @Test
    void projectWithoutRecordsIsIdle() {
        when(buildRecordRepo.findTopByProjectIdOrderByStartedAtDesc(PROJECT_ID)).thenReturn(Optional.empty());

        assertThat(buildOrchestrator.getCurrentBuildStatus(PROJECT_ID)).isEqualTo(BuildStatus.IDLE);
    }
}
