package org.remus.docsportal.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

/** Resolves isolated on-disk locations for each project. */
@Component
public class ProjectPaths {

    @Value("${docs-portal.workspace-dir}")
    private Path workspaceDir;

    @Value("${docs-portal.mkdocs-config-dir}")
    private Path mkdocsConfigDir;

    @Value("${docs-portal.mkdocs-working-dir}")
    private Path mkdocsWorkingDir;

    @Value("${docs-portal.site-dir}")
    private Path siteDir;

    public Path workspace(String projectId) {
        return scoped(workspaceDir, projectId);
    }

    public Path mkdocsConfig(String projectId) {
        return scoped(mkdocsConfigDir, projectId);
    }

    public Path mkdocsWorking(String projectId) {
        return scoped(mkdocsWorkingDir, projectId);
    }

    public Path site(String projectId) {
        return scoped(siteDir, projectId);
    }

    private Path scoped(Path root, String projectId) {
        // Keep the original directory layout usable for existing installations.
        return ProjectService.DEFAULT_PROJECT_ID.equals(projectId) ? root : root.resolve(projectId);
    }
}
