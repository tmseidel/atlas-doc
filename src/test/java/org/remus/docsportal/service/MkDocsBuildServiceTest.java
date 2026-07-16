package org.remus.docsportal.service;

import org.remus.docsportal.model.entity.BuildRecord;
import org.remus.docsportal.model.entity.BuildStatus;
import org.remus.docsportal.model.entity.BuildTrigger;
import org.remus.docsportal.model.entity.Project;
import org.remus.docsportal.repository.BuildRecordJpaRepository;
import org.remus.docsportal.repository.RepositoryConfigJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MkDocsBuildServiceTest {

    @Mock
    private BuildRecordJpaRepository buildRecordRepo;

    @Mock
    private RepositoryConfigJpaRepository repoDao;

    @Mock
    private BuildHistoryService buildHistoryService;

    @Mock
    private ProjectService projectService;

    @Mock
    private ProjectPaths projectPaths;

    @InjectMocks
    private MkDocsBuildService buildService;

    @TempDir
    Path workspaceDir;

    @TempDir
    Path mkdocsWorkingDir;

    @TempDir
    Path siteDir;

    @TempDir
    Path mkdocsConfigDir;

    @BeforeEach
    void setUp() throws IOException {
        when(projectPaths.workspace("project-1")).thenReturn(workspaceDir);
        when(projectPaths.mkdocsWorking("project-1")).thenReturn(mkdocsWorkingDir);
        when(projectPaths.site("project-1")).thenReturn(siteDir);
        when(projectPaths.mkdocsConfig("project-1")).thenReturn(mkdocsConfigDir);
        when(projectService.getRequired("project-1")).thenReturn(Project.builder().id("project-1").name("Project").build());
        Files.writeString(mkdocsConfigDir.resolve("mkdocs.yml"), "site_name: test\n");
        Path mkdocsScript = workspaceDir.resolve("fake-mkdocs");
        Files.writeString(mkdocsScript, """
            #!/bin/sh
            while [ $# -gt 0 ]; do
              if [ "$1" = "--site-dir" ]; then
                shift
                mkdir -p "$1"
                printf '<html>test site</html>' > "$1/index.html"
                exit 0
              fi
              shift
            done
            exit 1
            """);
        mkdocsScript.toFile().setExecutable(true);
        ReflectionTestUtils.setField(buildService, "mkdocsCommand", mkdocsScript.toString());
    }

    @Test
    void runBuild_shouldCreateBuildRecords() {
        when(repoDao.findByProjectIdAndEnabledTrue("project-1")).thenReturn(java.util.List.of());

        buildService.runBuild("manual", "project-1");

        // Service saves: 1x record at start (RUNNING), 1x at end (SUCCESS)
        ArgumentCaptor<BuildRecord> captor = ArgumentCaptor.forClass(BuildRecord.class);
        verify(buildRecordRepo, atLeastOnce()).save(captor.capture());

        var records = captor.getAllValues();
        assertThat(records).hasSizeGreaterThanOrEqualTo(1);
        // At least one record should have MANUAL trigger
        assertThat(records).anyMatch(r -> r.getTrigger() == BuildTrigger.MANUAL);
        assertThat(records).anyMatch(r -> r.getStatus() == BuildStatus.SUCCESS);
    }

    @Test
    void runBuild_shouldCreateDocsAndConfigDirs() {
        when(repoDao.findByProjectIdAndEnabledTrue("project-1")).thenReturn(java.util.List.of());

        buildService.runBuild("manual", "project-1");

        // Verify the working directory was prepared (docs/ should exist after run)
        assertThat(mkdocsWorkingDir.resolve("docs")).exists();
    }

    @Test
    void runBuild_failureKeepsPreviousSuccessfulSite() throws IOException {
        when(repoDao.findByProjectIdAndEnabledTrue("project-1")).thenReturn(java.util.List.of());
        buildService.runBuild("manual", "project-1");

        Path mkdocsScript = workspaceDir.resolve("fake-mkdocs");
        Files.writeString(mkdocsScript, "#!/bin/sh\nexit 0\n");
        mkdocsScript.toFile().setExecutable(true);
        buildService.runBuild("manual", "project-1");

        assertThat(siteDir.resolve("index.html")).hasContent("<html>test site</html>");
        ArgumentCaptor<BuildRecord> captor = ArgumentCaptor.forClass(BuildRecord.class);
        verify(buildRecordRepo, atLeast(4)).save(captor.capture());
        assertThat(captor.getAllValues()).anyMatch(record -> record.getStatus() == BuildStatus.FAILED);
    }
}
