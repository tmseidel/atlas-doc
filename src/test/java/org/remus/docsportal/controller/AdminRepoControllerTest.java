package org.remus.docsportal.controller;

import org.remus.docsportal.model.entity.RepositoryConfig;
import org.remus.docsportal.repository.RepositoryConfigJpaRepository;
import org.remus.docsportal.service.ProjectService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@SpringBootTest
class AdminRepoControllerTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private RepositoryConfigJpaRepository repoDao;

    @Autowired
    private ProjectService projectService;

    private MockMvc mockMvc() {
        return webAppContextSetup(context).build();
    }

    @AfterEach
    void cleanup() {
        repoDao.deleteAll();
    }

    @Test
    void listRepos_empty_returnsEmptyArray() throws Exception {
        mockMvc().perform(get("/api/repositories"))
            .andExpect(status().isOk())
            .andExpect(content().json("[]"));
    }

    @Test
    void createRepo_shouldPersistAndReturn() throws Exception {
        mockMvc().perform(post("/api/repositories")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "name": "test-repo",
                      "cloneUrl": "https://gitea.example.com/org/test.git",
                      "branch": "main",
                      "subdirectories": ["docs", "wiki"],
                      "enabled": true
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("test-repo"))
            .andExpect(jsonPath("$.cloneUrl").value("https://gitea.example.com/org/test.git"))
            .andExpect(jsonPath("$.branch").value("main"))
            .andExpect(jsonPath("$.enabled").value(true));
    }

    @Test
    void listRepos_afterCreate_returnsRepo() throws Exception {
        var repo = new RepositoryConfig();
        repo.setName("list-test");
        repo.setProject(projectService.getDefaultProject());
        repo.setCloneUrl("https://example.com/repo.git");
        repo.setSubdirectoriesJson("[\"docs\"]");
        repo.setEnabled(true);
        repoDao.save(repo);

        mockMvc().perform(get("/api/repositories"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].name").value("list-test"));
    }

    @Test
    void deleteRepo_shouldReturn404WhenNotFound() throws Exception {
        mockMvc().perform(delete("/api/repositories/nonexistent-id"))
            .andExpect(status().isNotFound());
    }

    @Test
    void updateRepo_shouldReturn404WhenNotFound() throws Exception {
        mockMvc().perform(put("/api/repositories/nonexistent-id")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "name": "updated",
                      "cloneUrl": "https://example.com/updated.git",
                      "subdirectories": ["docs"]
                    }
                    """))
            .andExpect(status().isNotFound());
    }
}
