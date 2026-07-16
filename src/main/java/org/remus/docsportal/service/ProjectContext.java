package org.remus.docsportal.service;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Stores the selected project in the authenticated browser session. */
@Component
@RequiredArgsConstructor
public class ProjectContext {

    public static final String SESSION_ATTRIBUTE = "docsPortalProjectId";

    private final HttpSession session;
    private final ProjectService projectService;

    public String getProjectId() {
        Object projectId = session.getAttribute(SESSION_ATTRIBUTE);
        if (projectId instanceof String id && !id.isBlank()) {
            return id;
        }
        // This fallback keeps non-browser clients and tests on the seeded default
        // project. Successful interactive login always sets a project explicitly.
        return projectService.getDefaultProject().getId();
    }

    public void selectProject(String projectId) {
        projectService.getRequired(projectId);
        session.setAttribute(SESSION_ATTRIBUTE, projectId);
    }
}
