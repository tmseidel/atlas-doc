package com.mdeg.docsportal.service;

import com.mdeg.docsportal.model.entity.RepositoryConfig;
import com.mdeg.docsportal.model.entity.SyncStatus;
import com.mdeg.docsportal.repository.MkDocsConfigRepoJpaRepository;
import com.mdeg.docsportal.repository.RepositoryConfigJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GitSyncServiceTest {

    @Mock private RepositoryConfigJpaRepository repoDao;
    @Mock private MkDocsConfigRepoJpaRepository mkdocsConfigDao;
    @Mock private ProjectPaths projectPaths;
    @InjectMocks private GitSyncService gitSyncService;
    @TempDir Path workspaceDir;
    @TempDir Path mkdocsConfigDir;

    @BeforeEach
    void setUp() {
        lenient().when(projectPaths.workspace("project-1")).thenReturn(workspaceDir);
    }

    @Test
    void noEnabledReposReturnsFalse() {
        when(repoDao.findByProjectIdAndEnabledTrue("project-1")).thenReturn(List.of());
        assertThat(gitSyncService.syncAllRepositories("project-1")).isFalse();
        verify(repoDao, never()).save(any());
    }

    @Test
    void failedProjectRepositoryIsRecordedAsFailed() {
        RepositoryConfig repo = RepositoryConfig.builder().id("1").name("test-repo")
            .cloneUrl("https://gitea.example.com/org/test-repo.git").branch("main")
            .subdirectoriesJson("[\"docs\"]").enabled(true).build();
        when(repoDao.findByProjectIdAndEnabledTrue("project-1")).thenReturn(List.of(repo));

        assertThat(gitSyncService.syncAllRepositories("project-1")).isFalse();
        verify(repoDao).save(argThat(item -> item.getLastSyncStatus() == SyncStatus.FAILED));
    }
}
