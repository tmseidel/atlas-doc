package com.mdeg.docsportal.controller;

import com.mdeg.docsportal.dto.RepositoryConfigDto;
import com.mdeg.docsportal.model.entity.RepositoryConfig;
import com.mdeg.docsportal.repository.RepositoryConfigJpaRepository;
import com.mdeg.docsportal.service.ProjectContext;
import com.mdeg.docsportal.service.ProjectService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.Git;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/repositories")
@RequiredArgsConstructor
@Slf4j
public class AdminRepoController {

    private final RepositoryConfigJpaRepository repoDao;
    private final ProjectContext projectContext;
    private final ProjectService projectService;

    @GetMapping
    public List<RepositoryConfigDto> listRepos() {
        return repoDao.findByProjectId(projectContext.getProjectId()).stream()
            .map(this::toDto)
            .collect(Collectors.toList());
    }

    @PostMapping
    public ResponseEntity<RepositoryConfigDto> createRepo(@Valid @RequestBody RepositoryConfigDto dto) {
        var entity = RepositoryConfig.builder()
            .project(projectService.getRequired(projectContext.getProjectId()))
            .name(dto.name())
            .cloneUrl(dto.cloneUrl())
            .branch(dto.branch() != null ? dto.branch() : "main")
            .subdirectoriesJson(serializeSubdirectories(dto.subdirectories()))
            .enabled(dto.enabled() != null ? dto.enabled() : true)
            .authToken(dto.authToken())
            .build();
        var saved = repoDao.save(entity);
        return ResponseEntity.ok(toDto(saved));
    }

    @PutMapping("/{id}")
    public ResponseEntity<RepositoryConfigDto> updateRepo(@PathVariable String id, @Valid @RequestBody RepositoryConfigDto dto) {
        var entity = repoDao.findById(id)
            .filter(repository -> repository.getProject().getId().equals(projectContext.getProjectId()))
            .orElseThrow(() -> new EntityNotFoundException("Repository not found: " + id));

        entity.setName(dto.name());
        entity.setCloneUrl(dto.cloneUrl());
        entity.setBranch(dto.branch() != null ? dto.branch() : "main");
        entity.setSubdirectoriesJson(serializeSubdirectories(dto.subdirectories()));
        if (dto.enabled() != null) {
            entity.setEnabled(dto.enabled());
        }
        // Only overwrite the token when the client sends a new, non-blank value.
        if (dto.authToken() != null && !dto.authToken().isBlank()) {
            entity.setAuthToken(dto.authToken());
        }

        var saved = repoDao.save(entity);
        return ResponseEntity.ok(toDto(saved));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRepo(@PathVariable String id) {
        var repository = repoDao.findById(id)
            .filter(item -> item.getProject().getId().equals(projectContext.getProjectId()));
        if (repository.isEmpty()) return ResponseEntity.notFound().build();
        repoDao.delete(repository.get());
        return ResponseEntity.noContent().build();
    }

    /**
     * REPO-005: Test connection to a Gitea repository before saving.
     * Accepts clone URL, branch, and optional auth token in request body.
     */
    @PostMapping("/test-connection")
    public ResponseEntity<?> testConnection(@RequestBody RepositoryConfigDto dto) {
        try {
            String branch = dto.branch() != null && !dto.branch().isBlank() ? dto.branch() : "main";
            String authToken = resolveAuthToken(dto);
            org.eclipse.jgit.api.LsRemoteCommand lsRemote = Git.lsRemoteRepository()
                .setRemote(dto.cloneUrl())
                .setHeads(true);
            if (authToken != null && !authToken.isBlank()) {
                lsRemote.setCredentialsProvider(
                    new org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider(
                        "oauth2", authToken));
            }
            var refs = lsRemote.call();
            boolean branchExists = refs.stream()
                .anyMatch(r -> r.getName().equals("refs/heads/" + branch));

            return ResponseEntity.ok(java.util.Map.of(
                "connected", true,
                "branchExists", branchExists,
                "message", branchExists ? "Repository and branch are accessible." : "Repository accessible but branch not found."
            ));
        } catch (Exception e) {
            log.warn("Connection test failed for {}: {}", dto.cloneUrl(), e.getMessage());
            return ResponseEntity.ok(java.util.Map.of(
                "connected", false,
                "message", e.getMessage()
            ));
        }
    }

    /** Uses a newly entered token when present; otherwise reuses the saved token for edits. */
    private String resolveAuthToken(RepositoryConfigDto dto) {
        if (dto.authToken() != null && !dto.authToken().isBlank()) {
            return dto.authToken();
        }
        if (dto.id() == null || dto.id().isBlank()) {
            return null;
        }
        return repoDao.findById(dto.id())
            .filter(repository -> repository.getProject().getId().equals(projectContext.getProjectId()))
            .map(RepositoryConfig::getAuthToken)
            .orElse(null);
    }

    private RepositoryConfigDto toDto(RepositoryConfig entity) {
        return new RepositoryConfigDto(
            entity.getId(),
            entity.getName(),
            entity.getCloneUrl(),
            entity.getBranch(),
            parseSubdirectories(entity.getSubdirectoriesJson()),
            entity.getEnabled(),
            entity.getAuthToken() != null && !entity.getAuthToken().isBlank(),
            null,  // never send the actual token to the client
            entity.getLastSyncAt(),
            entity.getLastSyncStatus(),
            entity.getLastSyncError()
        );
    }

    private String serializeSubdirectories(List<String> subdirectories) {
        if (subdirectories == null || subdirectories.isEmpty()) {
            return "[\"docs\"]";
        }
        return "[" + subdirectories.stream()
            .map(s -> "\"" + s.replace("\"", "\\\"") + "\"")
            .collect(Collectors.joining(",")) + "]";
    }

    private List<String> parseSubdirectories(String json) {
        if (json == null || json.isBlank()) {
            return List.of("docs");
        }
        String trimmed = json.trim();
        if (!trimmed.startsWith("[") || !trimmed.endsWith("]")) {
            return List.of("docs");
        }
        String inner = trimmed.substring(1, trimmed.length() - 1);
        if (inner.isBlank()) {
            return List.of();
        }
        return java.util.Arrays.stream(inner.split(","))
            .map(s -> s.trim().replaceAll("^\"|\"$", ""))
            .filter(s -> !s.isEmpty())
            .toList();
    }
}
