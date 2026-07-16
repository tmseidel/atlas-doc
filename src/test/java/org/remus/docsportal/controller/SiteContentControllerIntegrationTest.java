package org.remus.docsportal.controller;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@SpringBootTest
class SiteContentControllerIntegrationTest {

    private static final String TEST_DIRECTORY = "site-content-controller-test";

    @Autowired
    private WebApplicationContext context;

    @Value("${docs-portal.site-dir}")
    private Path siteDir;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() throws IOException {
        mockMvc = webAppContextSetup(context)
            .apply(SecurityMockMvcConfigurers.springSecurity())
            .build();
        Files.createDirectories(siteDir.resolve(TEST_DIRECTORY).resolve("nested-page"));
        Files.writeString(siteDir.resolve(TEST_DIRECTORY).resolve("nested-page/index.html"), "nested page");
    }

    @AfterEach
    void tearDown() throws IOException {
        Path testDirectory = siteDir.resolve(TEST_DIRECTORY);
        if (Files.exists(testDirectory)) {
            try (var paths = Files.walk(testDirectory)) {
                paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                    try {
                        Files.delete(path);
                    } catch (IOException e) {
                        throw new IllegalStateException(e);
                    }
                });
            }
        }
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    void directoryUrlServesMkDocsIndexFile() throws Exception {
        mockMvc.perform(get("/site/" + TEST_DIRECTORY + "/nested-page/"))
            .andExpect(status().isOk())
            .andExpect(content().string("nested page"));
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    void missingGeneratedPageReturnsNotFound() throws Exception {
        mockMvc.perform(get("/site/" + TEST_DIRECTORY + "/missing/"))
            .andExpect(status().isNotFound());
    }
}
