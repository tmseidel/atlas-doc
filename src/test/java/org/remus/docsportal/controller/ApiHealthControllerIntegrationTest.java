package org.remus.docsportal.controller;

import org.remus.docsportal.model.entity.BuildRecord;
import org.remus.docsportal.model.entity.BuildStatus;
import org.remus.docsportal.model.entity.BuildTrigger;
import org.remus.docsportal.model.entity.RepositoryConfig;
import org.remus.docsportal.model.entity.SyncStatus;
import org.remus.docsportal.repository.BuildRecordJpaRepository;
import org.remus.docsportal.repository.RepositoryConfigJpaRepository;
import org.remus.docsportal.service.ProjectService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

import java.time.Instant;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@SpringBootTest
class ApiHealthControllerIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private RepositoryConfigJpaRepository repoDao;

    @Autowired
    private BuildRecordJpaRepository buildRecordRepo;

    @Autowired
    private ProjectService projectService;

    private MockMvc mockMvc() {
        return webAppContextSetup(context).build();
    }

    @AfterEach
    void cleanup() {
        buildRecordRepo.deleteAll();
        repoDao.deleteAll();
    }

    @Test
    void health_shouldReturnBuildStatus() throws Exception {
        mockMvc().perform(get("/api/health"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.buildStatus").value("IDLE"));
    }

    @Test
    void health_shouldIncludeRepoStatuses() throws Exception {
        var repo = new RepositoryConfig();
        repo.setName("health-test-repo");
        repo.setProject(projectService.getDefaultProject());
        repo.setCloneUrl("https://example.com/repo.git");
        repo.setSubdirectoriesJson("[\"docs\"]");
        repo.setEnabled(true);
        repo.setLastSyncStatus(SyncStatus.SUCCESS);
        repo.setLastSyncAt(Instant.now());
        repoDao.save(repo);

        mockMvc().perform(get("/api/health"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.repoStatuses").isArray())
            .andExpect(jsonPath("$.repoStatuses[?(@.name=='health-test-repo')].lastSyncStatus").value("SUCCESS"));
    }

    @Test
    void docPages_shouldReturnPages() throws Exception {
        mockMvc().perform(get("/api/doc-pages"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray());
    }

    @Test
    void buildStatus_shouldReturnIdle() throws Exception {
        mockMvc().perform(get("/api/build/status"))
            .andExpect(status().isOk())
            .andExpect(content().string("\"IDLE\""));
    }

    @Test
    void buildLog_shouldReturnEmpty() throws Exception {
        mockMvc().perform(get("/api/build/log"))
            .andExpect(status().isOk());
    }

    @Test
    void buildHistory_shouldReturnEmptyArray() throws Exception {
        mockMvc().perform(get("/api/build/history"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray());
    }

    @Test
    void buildHistory_shouldNotSerializeLazyProjectAssociation() throws Exception {
        BuildRecord record = buildRecordRepo.save(BuildRecord.builder()
            .project(projectService.getDefaultProject())
            .status(BuildStatus.SUCCESS)
            .startedAt(Instant.parse("2100-01-01T00:00:00Z"))
            .finishedAt(Instant.parse("2100-01-01T00:00:01Z"))
            .trigger(BuildTrigger.MANUAL)
            .build());

        mockMvc().perform(get("/api/build/history"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(record.getId()))
            .andExpect(jsonPath("$[0].project").doesNotExist())
            .andExpect(jsonPath("$[0].status").value("SUCCESS"));
    }
}
