package org.remus.docsportal.service;

import org.remus.docsportal.dto.MkDocsConfigRepoDto;
import org.remus.docsportal.model.entity.MkDocsConfigRepo;
import org.remus.docsportal.repository.MkDocsConfigRepoJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class MkDocsConfigService {

    private final MkDocsConfigRepoJpaRepository mkdocsConfigDao;
    private final GitSyncService gitSyncService;
    private final ProjectContext projectContext;
    private final ProjectService projectService;

    public MkDocsConfigRepoDto findConfig() {
        return mkdocsConfigDao.findByProjectId(projectContext.getProjectId())
            .map(this::toDto)
            .orElse(null);
    }

    @Transactional
    public MkDocsConfigRepoDto save(MkDocsConfigRepoDto dto) {
        String projectId = projectContext.getProjectId();
        MkDocsConfigRepo entity = mkdocsConfigDao.findByProjectId(projectId)
            .orElseGet(MkDocsConfigRepo::new);
        if (entity.getProject() == null) {
            entity.setProject(projectService.getRequired(projectId));
        }
        entity.setCloneUrl(dto.cloneUrl());
        entity.setBranch(dto.branch() == null || dto.branch().isBlank() ? "main" : dto.branch());
        if (dto.authToken() != null && !dto.authToken().isBlank() && !dto.authToken().equals("••••••••")) {
            entity.setAuthToken(dto.authToken());
        }
        MkDocsConfigRepo saved = mkdocsConfigDao.save(entity);
        log.info("MkDocs config repo updated for project {}", projectId);
        return toDto(saved);
    }

    public void triggerSync() {
        gitSyncService.syncMkDocsConfigRepository(projectContext.getProjectId());
    }

    private MkDocsConfigRepoDto toDto(MkDocsConfigRepo entity) {
        return new MkDocsConfigRepoDto(entity.getId(), entity.getCloneUrl(), entity.getBranch(),
            entity.getAuthToken() == null ? null : "••••••••");
    }
}
