package com.mdeg.docsportal.service;

import com.mdeg.docsportal.model.entity.BuildRecord;
import com.mdeg.docsportal.model.entity.BuildStatus;
import com.mdeg.docsportal.model.entity.BuildTrigger;
import com.mdeg.docsportal.model.entity.RepositoryConfig;
import com.mdeg.docsportal.repository.BuildRecordJpaRepository;
import com.mdeg.docsportal.repository.RepositoryConfigJpaRepository;
import com.mdeg.docsportal.util.PathSanitizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Slf4j
public class MkDocsBuildService {

    private final BuildRecordJpaRepository buildRecordRepo;
    private final RepositoryConfigJpaRepository repoDao;
    private final BuildHistoryService buildHistoryService;
    private final ProjectService projectService;
    private final ProjectPaths projectPaths;

    @Value("${docs-portal.mkdocs-command:mkdocs}")
    private String mkdocsCommand;

    public synchronized void runBuild(String triggerType, String projectId) {
        BuildRecord record = BuildRecord.builder()
            .project(projectService.getRequired(projectId))
            .status(BuildStatus.RUNNING)
            .startedAt(Instant.now())
            .trigger(BuildTrigger.valueOf(triggerType.toUpperCase()))
            .build();
        buildRecordRepo.save(record);

        StringBuilder logOutput = new StringBuilder();
        Path site = projectPaths.site(projectId);
        Path output = buildOutputDir(site);
        try {
            Path workspace = projectPaths.workspace(projectId);
            Path working = projectPaths.mkdocsWorking(projectId);
            Path config = projectPaths.mkdocsConfig(projectId);
            prepareDocsDirectory(projectId, workspace, working);
            mergeConfigRepoDocs(config, working);
            copyMkDocsConfig(config, working);

            deleteRecursively(output);
            Process process = new ProcessBuilder(mkdocsCommand, "build",
                "--config-file", working.resolve("mkdocs.yml").toString(),
                "--site-dir", output.toString())
                .directory(working.toFile())
                .redirectErrorStream(true)
                .start();
            logOutput.append(new String(process.getInputStream().readAllBytes()));
            int exitCode = process.waitFor();

            if (exitCode == 0) {
                publishSite(site, output);
                record.setStatus(BuildStatus.SUCCESS);
            } else {
                deleteRecursively(output);
                record.setStatus(BuildStatus.FAILED);
            }
        } catch (Exception e) {
            log.error("Build failed for project {}", projectId, e);
            logOutput.append("ERROR: ").append(e.getMessage());
            try {
                deleteRecursively(output);
            } catch (IOException cleanupFailure) {
                log.warn("Could not remove incomplete output: {}", cleanupFailure.getMessage());
            }
            record.setStatus(BuildStatus.FAILED);
        }

        record.setLogOutput(logOutput.toString());
        record.setFinishedAt(Instant.now());
        buildRecordRepo.save(record);
        pruneBuildHistory(projectId);
    }

    private void pruneBuildHistory(String projectId) {
        try {
            buildHistoryService.retainLatestTwenty(projectId);
        } catch (RuntimeException e) {
            log.warn("Could not prune project {} build history: {}", projectId, e.getMessage());
        }
    }

    private void prepareDocsDirectory(String projectId, Path workspace, Path working) throws IOException {
        Path docs = working.resolve("docs");
        deleteRecursively(docs);
        Files.createDirectories(docs);
        for (RepositoryConfig repo : repoDao.findByProjectIdAndEnabledTrue(projectId)) {
            Path repoDocs = docs.resolve(repo.getName());
            Files.createDirectories(repoDocs);
            for (String subdirectory : parseSubdirectories(repo.getSubdirectoriesJson())) {
                Path source = workspace.resolve(repo.getName()).resolve(subdirectory).normalize();
                if (!source.startsWith(workspace) || !Files.exists(source)) continue;
                String targetName = PathSanitizer.sanitize(subdirectory);
                Path target = uniquePath(repoDocs.resolve(targetName));
                copyDirectory(source, target);
            }
        }
    }

    private void mergeConfigRepoDocs(Path config, Path working) throws IOException {
        Path configDocs = config.resolve("docs");
        if (!Files.exists(configDocs)) return;
        Path targetDocs = working.resolve("docs");
        try (Stream<Path> paths = Files.walk(configDocs)) {
            for (Path source : paths.filter(Files::isRegularFile).toList()) {
                Path target = targetDocs.resolve(configDocs.relativize(source));
                Files.createDirectories(target.getParent());
                Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    private void copyMkDocsConfig(Path config, Path working) throws IOException {
        Path mkdocsYml = config.resolve("mkdocs.yml");
        if (!Files.isRegularFile(mkdocsYml)) {
            throw new IOException("MkDocs configuration is missing mkdocs.yml: " + mkdocsYml);
        }
        Files.createDirectories(working);
        clearWorkingConfig(working);
        Files.copy(mkdocsYml, working.resolve("mkdocs.yml"), StandardCopyOption.REPLACE_EXISTING);
        try (Stream<Path> entries = Files.list(config)) {
            for (Path source : entries.toList()) {
                String name = source.getFileName().toString();
                if (name.equals(".git") || name.equals("docs") || name.equals("mkdocs.yml")) continue;
                Path target = working.resolve(name);
                if (Files.isDirectory(source)) copyDirectory(source, target);
                else Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    private void publishSite(Path site, Path output) throws IOException {
        verifySite(output, "new build output");
        Files.createDirectories(site);
        Path backup = site.resolveSibling(site.getFileName() + ".previous");
        deleteRecursively(backup);
        copyDirectory(site, backup);
        try {
            deleteDirectoryContents(site);
            copyDirectory(output, site);
            verifySite(site, "published site");
        } catch (Exception failure) {
            deleteDirectoryContents(site);
            copyDirectory(backup, site);
            throw failure;
        } finally {
            deleteRecursively(output);
            deleteRecursively(backup);
        }
    }

    private Path buildOutputDir(Path site) {
        return site.resolveSibling(site.getFileName() + ".new");
    }

    private Path uniquePath(Path target) {
        int suffix = 1;
        Path candidate = target;
        while (Files.exists(candidate)) candidate = target.resolveSibling(target.getFileName() + "_" + suffix++);
        return candidate;
    }

    private void clearWorkingConfig(Path working) throws IOException {
        try (Stream<Path> entries = Files.list(working)) {
            for (Path entry : entries.toList()) {
                if (!entry.getFileName().toString().equals("docs")) deleteRecursively(entry);
            }
        }
    }

    private void verifySite(Path directory, String description) throws IOException {
        if (!Files.isRegularFile(directory.resolve("index.html"))) {
            throw new IOException("Build verification failed: index.html missing from " + description);
        }
    }

    private void copyDirectory(Path source, Path target) throws IOException {
        Files.createDirectories(target);
        try (Stream<Path> paths = Files.walk(source)) {
            for (Path item : paths.toList()) {
                Path destination = target.resolve(source.relativize(item));
                if (Files.isDirectory(item)) Files.createDirectories(destination);
                else Files.copy(item, destination, StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    private void deleteDirectoryContents(Path directory) throws IOException {
        if (!Files.isDirectory(directory)) return;
        try (Stream<Path> entries = Files.list(directory)) {
            for (Path entry : entries.toList()) deleteRecursively(entry);
        }
    }

    private void deleteRecursively(Path path) throws IOException {
        if (!Files.exists(path)) return;
        try (Stream<Path> paths = Files.walk(path)) {
            for (Path item : paths.sorted(Comparator.reverseOrder()).toList()) Files.delete(item);
        }
    }

    private List<String> parseSubdirectories(String json) {
        if (json == null || json.isBlank()) return List.of("docs");
        String trimmed = json.trim();
        if (!trimmed.startsWith("[") || !trimmed.endsWith("]")) return List.of("docs");
        String contents = trimmed.substring(1, trimmed.length() - 1);
        if (contents.isBlank()) return List.of();
        List<String> result = java.util.Arrays.stream(contents.split(","))
            .map(value -> value.trim().replaceAll("^\"|\"$", ""))
            .filter(value -> !value.isEmpty())
            .toList();
        return result.isEmpty() ? List.of("docs") : result;
    }
}
