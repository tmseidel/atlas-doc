package com.mdeg.docsportal.controller;

import com.mdeg.docsportal.model.entity.RepositoryConfig;
import com.mdeg.docsportal.model.entity.SyncStatus;
import com.mdeg.docsportal.repository.RepositoryConfigJpaRepository;
import com.mdeg.docsportal.service.ProjectService;
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
    private ProjectService projectService;

    private MockMvc mockMvc() {
        return webAppContextSetup(context).build();
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
}
