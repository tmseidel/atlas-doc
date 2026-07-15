package com.mdeg.docsportal.service;

import com.mdeg.docsportal.model.entity.MkDocsConfigRepo;
import com.mdeg.docsportal.model.entity.RepositoryConfig;
import com.mdeg.docsportal.model.entity.SyncStatus;
import com.mdeg.docsportal.repository.MkDocsConfigRepoJpaRepository;
import com.mdeg.docsportal.repository.RepositoryConfigJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class GitSyncService {

    private final RepositoryConfigJpaRepository repoDao;
    private final MkDocsConfigRepoJpaRepository mkdocsConfigDao;
    private final ProjectPaths projectPaths;

    public boolean syncAllRepositories(String projectId) {
        boolean anyChanged = false;
        for (RepositoryConfig repo : repoDao.findByProjectIdAndEnabledTrue(projectId)) {
            try {
                boolean changed = pullRepository(repo.getCloneUrl(), repo.getBranch(),
                    projectPaths.workspace(projectId).resolve(repo.getName()), repo.getName(),
                    repo.getAuthToken(), repo.getSshKey(), projectPaths.workspace(projectId));
                repo.setLastSyncAt(Instant.now());
                repo.setLastSyncStatus(SyncStatus.SUCCESS);
                repo.setLastSyncError(null);
                repoDao.save(repo);
                anyChanged |= changed;
            } catch (Exception e) {
                log.error("Failed to sync project {} repository {}: {}", projectId, repo.getName(), e.getMessage());
                repo.setLastSyncAt(Instant.now());
                repo.setLastSyncStatus(SyncStatus.FAILED);
                repo.setLastSyncError(e.getMessage());
                repoDao.save(repo);
            }
        }
        return anyChanged;
    }

    public boolean syncMkDocsConfigRepository(String projectId) {
        return mkdocsConfigDao.findByProjectId(projectId)
            .map(config -> syncMkDocsConfigRepository(projectId, config))
            .orElse(false);
    }

    private boolean syncMkDocsConfigRepository(String projectId, MkDocsConfigRepo config) {
        try {
            Path checkout = projectPaths.mkdocsConfig(projectId);
            String oldHead = getHeadSha(checkout);
            if (Files.exists(checkout.resolve(".git"))) {
                try (Git git = Git.open(checkout.toFile())) {
                    git.pull().setRemoteBranchName(config.getBranch())
                        .setTransportConfigCallback(tc -> {
                            if (tc instanceof org.eclipse.jgit.transport.Transport transport) {
                                applyCredentials(transport, config.getCloneUrl(), config.getAuthToken(), null, checkout);
                            }
                        }).call();
                }
            } else {
                Git.cloneRepository().setURI(config.getCloneUrl()).setBranch(config.getBranch())
                    .setDirectory(checkout.toFile())
                    .setTransportConfigCallback(tc -> {
                        if (tc instanceof org.eclipse.jgit.transport.Transport transport) {
                            applyCredentials(transport, config.getCloneUrl(), config.getAuthToken(), null, checkout);
                        }
                    }).call();
            }
            boolean changed = !Objects.equals(oldHead, getHeadSha(checkout));
            config.setLastSyncAt(Instant.now());
            config.setLastSyncStatus(SyncStatus.SUCCESS);
            config.setLastSyncError(null);
            mkdocsConfigDao.save(config);
            return changed;
        } catch (Exception e) {
            log.error("Failed to sync MkDocs config for project {}: {}", projectId, e.getMessage());
            config.setLastSyncStatus(SyncStatus.FAILED);
            config.setLastSyncError(e.getMessage());
            mkdocsConfigDao.save(config);
            return false;
        }
    }

    private boolean pullRepository(String url, String branch, Path targetDir, String repoName,
                                   String authToken, String sshKey, Path projectWorkspace) throws Exception {
        String oldHead = getHeadSha(targetDir);
        if (Files.exists(targetDir.resolve(".git"))) {
            try (Git git = Git.open(targetDir.toFile())) {
                git.pull().setRemoteBranchName(branch).setTransportConfigCallback(tc -> {
                    if (tc instanceof org.eclipse.jgit.transport.Transport transport) {
                        applyCredentials(transport, url, authToken, sshKey, projectWorkspace);
                    }
                }).call();
            }
        } else {
            CloneCommand command = Git.cloneRepository().setURI(url).setBranch(branch).setDirectory(targetDir.toFile());
            command.setTransportConfigCallback(tc -> {
                if (tc instanceof org.eclipse.jgit.transport.Transport transport) {
                    applyCredentials(transport, url, authToken, sshKey, projectWorkspace);
                }
            });
            command.call();
        }
        return !Objects.equals(oldHead, getHeadSha(targetDir));
    }

    private void applyCredentials(org.eclipse.jgit.transport.Transport transport, String url,
                                  String authToken, String sshKey, Path projectWorkspace) {
        if ((url.startsWith("https://") || url.startsWith("http://")) && authToken != null && !authToken.isBlank()) {
            transport.setCredentialsProvider(new UsernamePasswordCredentialsProvider("oauth2", authToken));
        } else if ((url.startsWith("git@") || url.startsWith("ssh://")) && sshKey != null && !sshKey.isBlank()) {
            try {
                Files.createDirectories(projectWorkspace);
                Path keyFile = projectWorkspace.resolve(".ssh_key");
                Files.writeString(keyFile, sshKey);
                keyFile.toFile().setReadable(true, true);
            } catch (IOException e) {
                log.warn("Could not write SSH key for project workspace: {}", e.getMessage());
            }
        }
    }

    private String getHeadSha(Path repoDir) {
        if (!Files.exists(repoDir.resolve(".git"))) return null;
        try (Git git = Git.open(repoDir.toFile())) {
            var head = git.getRepository().resolve("HEAD");
            return head == null ? null : head.getName();
        } catch (IOException e) {
            return null;
        }
    }
}
